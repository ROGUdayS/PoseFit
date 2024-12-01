package com.example.pose_fit

//val currentTime = System.currentTimeMillis()
//
//// Check if 20 seconds have passed since the last execution
//if (currentTime - lastExecutionTime >= 20000) {  // 20 seconds = 20000 milliseconds
//    // Update the last execution time
//    lastExecutionTime = currentTime
//}

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.max
import android.speech.tts.TextToSpeech
import java.util.Locale
private var lastExecutionTime: Long = 0


class OverlayView(context: Context, attrs: AttributeSet?) :
    View(context, attrs), TextToSpeech.OnInitListener {

    private var results: PoseLandmarkerResult? = null
    private var pointPaint = Paint()
    private var linePaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    private var workoutType: String = "Full_Body"
    private val warnings = mutableListOf<String>()
    private var isTimerRunning = false

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var lastWarningTimes = mutableMapOf<String, Long>()
    var isResetTriggerred = false

    private var lStage: String = ""
    private var lCounter: Int = 0
    private var lCorrectCounter: Int = 0
    private var lIncorrectCounter: Int = 0
    private var lIsCorrect: Boolean = true

    private var rStage: String = ""
    private var rCounter: Int = 0
    private var rCorrectCounter: Int = 0
    private var rIncorrectCounter: Int = 0
    private var rIsCorrect: Boolean = true

    private var stage = 1
    private var count = 0
    private var correct = 0
    private var incorrect = 0
    private var check = true


    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("OverlayView", "This Language is not supported")
            }
        } else {
            Log.e("OverlayView", "Initialization Failed!")
        }
    }

    init {
        initPaints()
    }

    fun setWorkoutType(type: String) {
        workoutType = type
        Log.d("OverlayView", "Workout Type Set to: $workoutType")
        invalidate()  // Redraw if necessary to reflect new workout type
    }

    fun setTimerState(type: Boolean){
        isTimerRunning=type
        Log.d("OverlayView", "Timer Running State Set to: $isTimerRunning")
        invalidate()
    }

    fun setResetState(type: Boolean){
        isResetTriggerred=type
        if(isResetTriggerred){
            lStage = ""
            lCounter = 0
            lCorrectCounter = 0
            lIncorrectCounter = 0
            lIsCorrect = true
            rStage = ""
            rCounter = 0
            rCorrectCounter = 0
            rIncorrectCounter = 0
            rIsCorrect = true
            Log.d("OverlayView", "Reset Triggered and values reset.")
            isResetTriggerred = false
        }
        Log.d("OverlayView", "Reset Triggered $isResetTriggerred")
        isResetTriggerred=false
        invalidate()
    }

    fun clear() {
        results = null
        pointPaint.reset()
        linePaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        linePaint.color = ContextCompat.getColor(context!!, R.color.white)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE
        pointPaint.color = Color.WHITE
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { poseLandmarkerResult ->
            when (workoutType) {
                "Full_Body" -> drawFullBody(canvas, poseLandmarkerResult)
                "bicep_curls" -> drawBicepCurls(canvas, poseLandmarkerResult)
                "shoulder_press" -> drawShoulderPress(canvas, poseLandmarkerResult)
                "squats" -> drawSquats(canvas, poseLandmarkerResult)
                else -> Log.d("OverlayView", "Unknown workout type: $workoutType")
            }
        }
    }

    private fun drawFullBody(canvas: Canvas, result: PoseLandmarkerResult) {
        drawCommonLandmarks(canvas, result)
    }

    private fun drawBicepCurls(canvas: Canvas, result: PoseLandmarkerResult) {

        Log.d("OverlayView", "Timer Running: $isTimerRunning")
        // Extract landmarks
        if (result.landmarks().isEmpty()) {
            Log.d("OverlayView", "No landmarks detected.")
            return
        }
        val landmarkPaint = Paint().apply {
            strokeWidth = OverlayView.LANDMARK_STROKE_WIDTH
            style = Paint.Style.FILL
        }

        val linePaint = Paint().apply {
            strokeWidth = OverlayView.LANDMARK_STROKE_WIDTH
            style = Paint.Style.STROKE
        }

        // Set colors based on timer running state
        if (isTimerRunning) {
            val landmarks = result.landmarks()[0] // Assuming one person detected, hence [0]
            // Left Biceps
            val leftShoulderIndices = listOf(24,12,14)
            val leftArmIndices = listOf(12, 14, 16)
            val leftWristIndices = listOf(14, 16, 20)

            val leftShoulderPoints = leftShoulderIndices.map { Pair(landmarks[it].x() * imageWidth * scaleFactor, landmarks[it].y() * imageHeight * scaleFactor) }
            val angleLeftShoulder = Calculations.calculateAngles(leftShoulderPoints[0], leftShoulderPoints[1], leftShoulderPoints[2])

            val leftArmPoints = leftArmIndices.map { Pair(landmarks[it].x() * imageWidth * scaleFactor, landmarks[it].y() * imageHeight * scaleFactor) }
            val angleLeftArm = Calculations.calculateAngles(leftArmPoints[0], leftArmPoints[1], leftArmPoints[2])

            val leftWristPoints = leftWristIndices.map { Pair(landmarks[it].x() * imageWidth * scaleFactor, landmarks[it].y() * imageHeight * scaleFactor) }
            val angleLeftWrist = Calculations.calculateAngles(leftWristPoints[0], leftWristPoints[1], leftWristPoints[2])
            val currentTime = System.currentTimeMillis()


            if (angleLeftShoulder in 5.0..25.0) {
                if (angleLeftWrist !in 150.0..190.0 && currentTime - lastExecutionTime >= 200) {
                    addWarning("Lock Wrists")
                    drawPointsAndLines(canvas, leftWristPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
                    lastExecutionTime = currentTime
                } else {
                    drawPointsAndLines(canvas, leftWristPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)
                }
                drawPointsAndLines(canvas, leftArmPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)
                if(angleLeftArm in 1.0..30.0 && lStage == "down"){
                    lStage = "up"
                    lCounter++
                    addWarning("Left Arm Down")
                    if (angleLeftArm in 10.0..20.0){drawPointsAndLines(canvas, leftArmPoints, landmarkPaint, linePaint, Color.GREEN, Color.GREEN)}
                    if(lIsCorrect){
                        lCorrectCounter++
                    }
                    else{
                        lIncorrectCounter++
                    }
                    lIsCorrect=true
                }
                else if(angleLeftArm>160){
                    lStage="down"
                    addWarning("Left Arm Up")
                    if (angleLeftArm in 165.0..180.0){drawPointsAndLines(canvas, leftArmPoints, landmarkPaint, linePaint, Color.GREEN, Color.GREEN)}

                }

            } else {
                lIsCorrect =false
                addWarning("Fix Left Shoulder Angle")
                drawPointsAndLines(canvas, leftShoulderPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
                drawPointsAndLines(canvas, leftArmPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
                drawPointsAndLines(canvas,leftWristPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
            }

            displayWarnings(canvas)
            // Optionally, display angles on the screen or log them
            Log.d("OverlayView", "Left Shoulder Angle: $angleLeftShoulder")
            Log.d("OverlayView", "Left Arm Angle: $angleLeftArm")
            Log.d("OverlayView", "Left Wrist Angle: $angleLeftWrist")
            val textPaintLeft = Paint().apply {
                color = Color.WHITE
                textSize = 40f // Adjust text size as needed
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.LEFT
            }
            // Draw counters on the screen
            canvas.drawText("",canvas.width - 10f, 100f, textPaintLeft)
            canvas.drawText("Total Reps: $lCounter", 10f, 100f, textPaintLeft) // Adjust x, y values as needed for layout
            canvas.drawText("Correct Reps: $lCorrectCounter", 10f, 150f, textPaintLeft)
            canvas.drawText("Incorrect Reps: $lIncorrectCounter", 10f, 200f, textPaintLeft)

            // Right Biceps
            val rightShoulderIndices = listOf(23, 11, 13)
            val rightArmIndices = listOf(11, 13, 15)
            val rightWristIndices = listOf(13, 15, 19)

            val rightShoulderPoints = rightShoulderIndices.map { Pair(landmarks[it].x() * imageWidth * scaleFactor, landmarks[it].y() * imageHeight * scaleFactor) }
            val angleRightShoulder = Calculations.calculateAngles(rightShoulderPoints[0], rightShoulderPoints[1], rightShoulderPoints[2])

            val rightArmPoints = rightArmIndices.map { Pair(landmarks[it].x() * imageWidth * scaleFactor, landmarks[it].y() * imageHeight * scaleFactor) }
            val angleRightArm = Calculations.calculateAngles(rightArmPoints[0], rightArmPoints[1], rightArmPoints[2])

            val rightWristPoints = rightWristIndices.map { Pair(landmarks[it].x() * imageWidth * scaleFactor, landmarks[it].y() * imageHeight * scaleFactor) }
            val angleRightWrist = Calculations.calculateAngles(rightWristPoints[0], rightWristPoints[1], rightWristPoints[2])
            if (angleRightShoulder in 5.0..25.0) {
                if (angleRightWrist !in 150.0..190.0 && currentTime - lastExecutionTime >= 200) {
                    addWarning("Lock Wrists")
                    drawPointsAndLines(canvas, rightWristPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
                    lastExecutionTime = currentTime
                } else {
                    drawPointsAndLines(canvas, rightWristPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)
                }
                drawPointsAndLines(canvas, rightArmPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)
                if(angleRightArm in 1.0..30.0 && rStage == "down"){
                    rStage = "up"
                    rCounter++
                    addWarning("Right Arm Down")
                    if (angleRightArm in 10.0..20.0){drawPointsAndLines(canvas, rightArmPoints, landmarkPaint, linePaint, Color.GREEN, Color.GREEN)}
                    if(rIsCorrect){
                        rCorrectCounter++
                    }
                    else{
                        rIncorrectCounter++
                    }
                    rIsCorrect = true
                }
                else if(angleRightArm > 160){
                    rStage = "down"
                    addWarning("Right Arm Up")
                    if (angleRightArm in 165.0..180.0){drawPointsAndLines(canvas, rightArmPoints, landmarkPaint, linePaint, Color.GREEN, Color.GREEN)}

                }
            } else {
                rIsCorrect = false
                addWarning("Fix Right Shoulder Angle")
                drawPointsAndLines(canvas, rightShoulderPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
                drawPointsAndLines(canvas, rightArmPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
                drawPointsAndLines(canvas, rightWristPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
            }

            displayWarnings(canvas)
            // Optionally, display angles on the screen or log them
            Log.d("OverlayView", "Right Shoulder Angle: $angleRightShoulder")
            Log.d("OverlayView", "Right Arm Angle: $angleRightArm")
            Log.d("OverlayView", "Right Wrist Angle: $angleRightWrist")
            val textPaintRight = Paint().apply {
                color = Color.WHITE
                textSize = 40f // Adjust text size as needed
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }
            // Draw counters on the screen
            canvas.drawText("Total Reps: $rCounter", canvas.width - 10f, 100f, textPaintRight) // Adjust x, y values as needed for layout
            canvas.drawText("Correct Reps: $rCorrectCounter", canvas.width - 10f, 150f, textPaintRight)
            canvas.drawText("Incorrect Reps: $rIncorrectCounter", canvas.width - 10f, 200f, textPaintRight)



        } else{

            drawCommonLandmarks(canvas, result)
        }

    }

    private fun drawShoulderPress(canvas: Canvas, result: PoseLandmarkerResult) {
        Log.d("OverlayView", "Timer Running: $isTimerRunning")
        // Extract landmarks
        if (result.landmarks().isEmpty()) {
            Log.d("OverlayView", "No landmarks detected.")
            return
        }
        // Set colors based on timer running state
        val landmarkPaint = Paint().apply {
            strokeWidth = OverlayView.LANDMARK_STROKE_WIDTH
            style = Paint.Style.FILL
        }

        val linePaint = Paint().apply {
            strokeWidth = OverlayView.LANDMARK_STROKE_WIDTH
            style = Paint.Style.STROKE
        }
        if (isTimerRunning) {

            val landmarks = result.landmarks()[0] // Assuming one person detected, hence [0]
            val leftArmIndices = listOf(11, 13, 15)
            val leftShoulderIndices = listOf(23,11,13)
            val leftShoulderPoints = leftShoulderIndices.map { Pair(landmarks[it].x() * imageWidth * scaleFactor, landmarks[it].y() * imageHeight * scaleFactor) }
            val angleLeftShoulder = Calculations.calculateAngles(leftShoulderPoints[0], leftShoulderPoints[1], leftShoulderPoints[2])
            val leftArmPoints = leftArmIndices.map { Pair(landmarks[it].x() * imageWidth * scaleFactor, landmarks[it].y() * imageHeight * scaleFactor) }
            val angleLeftArm = Calculations.calculateAngles(leftArmPoints[0], leftArmPoints[1], leftArmPoints[2])

            val rightArmIndices = listOf(12, 14, 16)
            val RightShoulderIndices = listOf(24,12,14)
            val rightShoulderPoints = RightShoulderIndices.map { Pair(landmarks[it].x() * imageWidth * scaleFactor, landmarks[it].y() * imageHeight * scaleFactor) }
            val angleRightShoulder = Calculations.calculateAngles(rightShoulderPoints[0], rightShoulderPoints[1], rightShoulderPoints[2])
            val rightArmPoints = rightArmIndices.map { Pair(landmarks[it].x() * imageWidth * scaleFactor, landmarks[it].y() * imageHeight * scaleFactor) }
            val angleRightArm = Calculations.calculateAngles(rightArmPoints[0], rightArmPoints[1], rightArmPoints[2])


            if(angleRightShoulder>50 && angleLeftShoulder>50){
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastExecutionTime >= 1) {  // 1 seconds = 1000 milliseconds
                    if (Math.abs(angleRightShoulder - angleLeftShoulder) <= 20){
                        if(angleRightArm in 50.0..70.00 && angleLeftArm in 50.0..70.00){
                            addWarning("Shoulders Down")
                            lStage="down"
                            drawPointsAndLines(canvas, rightShoulderPoints, landmarkPaint, linePaint, Color.GREEN, Color.GREEN)
                            drawPointsAndLines(canvas, leftShoulderPoints, landmarkPaint, linePaint, Color.GREEN, Color.GREEN)
                            drawPointsAndLines(canvas, rightArmPoints, landmarkPaint, linePaint, Color.GREEN, Color.GREEN)
                            drawPointsAndLines(canvas, leftArmPoints, landmarkPaint, linePaint, Color.GREEN, Color.GREEN)
                        }

                        if(angleRightShoulder > 130 && angleLeftShoulder > 130 && angleLeftArm>135 && angleRightArm>135 && lStage == "down"){
                            addWarning("Shoulders Up")
                            lStage="up"
                            lCounter++
                            if (lIsCorrect){
                                lCorrectCounter++
                            }
                            else{
                                addWarning("Entering incorrect")
                                lIncorrectCounter++
                                lIsCorrect = true
                            }
                            drawPointsAndLines(canvas, rightShoulderPoints, landmarkPaint, linePaint, Color.GREEN, Color.GREEN)
                            drawPointsAndLines(canvas, leftShoulderPoints, landmarkPaint, linePaint, Color.GREEN, Color.GREEN)
                            drawPointsAndLines(canvas, rightArmPoints, landmarkPaint, linePaint, Color.GREEN, Color.GREEN)
                            drawPointsAndLines(canvas, leftArmPoints, landmarkPaint, linePaint, Color.GREEN, Color.GREEN)
                            addWarning(if(angleLeftArm > 175 && angleRightArm > 175) "Dont Lock Elbows" else "")
                        }

                        else{
                            drawPointsAndLines(canvas, rightShoulderPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)
                            drawPointsAndLines(canvas, leftShoulderPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)
                            drawPointsAndLines(canvas, rightArmPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)
                            drawPointsAndLines(canvas, leftArmPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)
                        }

                    }

                    else{
                        lIsCorrect = false
                        drawPointsAndLines(canvas, rightShoulderPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
                        drawPointsAndLines(canvas, leftShoulderPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
                        drawPointsAndLines(canvas, rightArmPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
                        drawPointsAndLines(canvas, leftArmPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
                    }
                    // Update the last execution time
                    lastExecutionTime = currentTime
                }
                else{
                    drawPointsAndLines(canvas, rightShoulderPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)
                    drawPointsAndLines(canvas, leftShoulderPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)
                    drawPointsAndLines(canvas, rightArmPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)
                    drawPointsAndLines(canvas, leftArmPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)
                }

            }else{
                lIsCorrect = false
                drawPointsAndLines(canvas, rightShoulderPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
                drawPointsAndLines(canvas, leftShoulderPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
                drawPointsAndLines(canvas, rightArmPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
                drawPointsAndLines(canvas, leftArmPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
            }

            // Optionally, display angles on the screen or log them
            Log.d("OverlayView", "Left Shoulder Angle: $angleLeftShoulder")
            Log.d("OverlayView", "Left Arm Angle: $angleLeftArm")
            Log.d("OverlayView", "Right Shoulder Angle: $angleRightShoulder")
            Log.d("OverlayView", "Right Arm Angle: $angleRightArm")

            displayWarnings(canvas)

            val textPaintLeft = Paint().apply {
                color = Color.WHITE
                textSize = 40f // Adjust text size as needed
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.LEFT
            }

            val textPaintAngles = Paint().apply {
                color = Color.WHITE
                textSize = 40f // Adjust text size as needed
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }

            // Calculate X coordinate for center alignment
            val centerX = canvas.width / 2f

            // Calculate Y coordinates for the text positions
            val baseY = canvas.height - 50f // Start position from the bottom
            val textSpacing = 50f // Space between lines

            // Format angles to one decimal place
            val angleLeftShoulderFormatted = String.format("%.1f", angleLeftShoulder)
            val angleRightShoulderFormatted = String.format("%.1f", angleRightShoulder)
            val angleLeftArmFormatted = String.format("%.1f", angleLeftArm)
            val angleRightArmFormatted = String.format("%.1f", angleRightArm)

            // Draw angles on screen
            canvas.drawText("Left Shoulder Angle: $angleLeftShoulderFormatted", centerX, baseY, textPaintAngles)
            canvas.drawText("Right Shoulder Angle: $angleRightShoulderFormatted", centerX, baseY - textSpacing, textPaintAngles)
            canvas.drawText("Left Arm Angle: $angleLeftArmFormatted", centerX, baseY - 2 * textSpacing, textPaintAngles)
            canvas.drawText("Right Arm Angle: $angleRightArmFormatted", centerX, baseY - 3 * textSpacing, textPaintAngles)

            // Draw counters on the screen
            canvas.drawText("",canvas.width - 10f, 100f, textPaintLeft)
            canvas.drawText("Total Reps: $lCounter", 10f, 100f, textPaintLeft) // Adjust x, y values as needed for layout
            canvas.drawText("Correct Reps: $lCorrectCounter", 10f, 150f, textPaintLeft)
            canvas.drawText("Incorrect Reps: $lIncorrectCounter", 10f, 200f, textPaintLeft)


        } else{drawCommonLandmarks(canvas, result)}

    }

    private fun drawSquats(canvas: Canvas, result: PoseLandmarkerResult) {
        Log.d("OverlayView", "Timer Running: $isTimerRunning")
        // Extract landmarks
        if (result.landmarks().isEmpty()) {
            Log.d("OverlayView", "No landmarks detected.")
            return
        }
        // Set colors based on timer running state
        val landmarkPaint = Paint().apply {
            strokeWidth = OverlayView.LANDMARK_STROKE_WIDTH
            style = Paint.Style.FILL
        }

        val linePaint = Paint().apply {
            strokeWidth = OverlayView.LANDMARK_STROKE_WIDTH
            style = Paint.Style.STROKE
        }
        if (isTimerRunning) {
            val landmarks = result.landmarks()[0]
            // Get the co-ordinates and calculate angle
            val l_hip = listOf(14, 24, 26)
            val leftHipPoints = l_hip.map { Pair(landmarks[it].x() * imageWidth * scaleFactor, landmarks[it].y() * imageHeight * scaleFactor) }
            val angleLeftHip = Calculations.calculateAngles(leftHipPoints[0], leftHipPoints[1], leftHipPoints[2])
            val l_knee = listOf(24, 26, 28)
            val leftKneePoints = l_knee.map { Pair(landmarks[it].x() * imageWidth * scaleFactor, landmarks[it].y() * imageHeight * scaleFactor) }
            val angleLeftKnee = Calculations.calculateAngles(leftKneePoints[0], leftKneePoints[1], leftKneePoints[2])

            val r_hip = listOf(13, 23, 25)
            val rightHipPoints = r_hip.map { Pair(landmarks[it].x() * imageWidth * scaleFactor, landmarks[it].y() * imageHeight * scaleFactor) }
            val angleRightHip = Calculations.calculateAngles(rightHipPoints[0], rightHipPoints[1], rightHipPoints[2])
            val r_knee = listOf(23, 25, 27)
            val rightKneePoints = r_knee.map { Pair(landmarks[it].x() * imageWidth * scaleFactor, landmarks[it].y() * imageHeight * scaleFactor) }
            val angleKneeShoulder = Calculations.calculateAngles(rightKneePoints[0], rightKneePoints[1], rightKneePoints[2])

            drawPointsAndLines(canvas, leftHipPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)
            drawPointsAndLines(canvas, leftKneePoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)

            drawPointsAndLines(canvas, rightHipPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)
            drawPointsAndLines(canvas, rightKneePoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)

            if (angleLeftHip > 174 && angleLeftKnee > 174) {
                if (stage == 1) {
                    stage = 0
                    count += 1

                    if (check) {
                        correct += 1
                    } else {
                        incorrect += 1
                        check = true  // Reset check flag for the next repetition

                    }
                }
            }

            // Check if the exercise is in the bent position
            if (angleLeftHip < 85) {
                stage = 1

                if (check && angleLeftHip <= 60) {
                    check = false  // Set check to false to indicate incorrect form
                    addWarning("Do not bend your spine")

                    drawPointsAndLines(canvas, leftHipPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
                    drawPointsAndLines(canvas, leftKneePoints, landmarkPaint, linePaint, Color.RED, Color.RED)

                    drawPointsAndLines(canvas, rightHipPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
                    drawPointsAndLines(canvas, rightKneePoints, landmarkPaint, linePaint, Color.RED, Color.RED)

                    if (angleLeftKnee < 125) {
                        addWarning("Maintain tension at hamstrings")
                    }
                }
            }

            if (angleRightHip > 174 && angleKneeShoulder > 174) {
                if (stage == 1) {
                    stage = 0
                    count += 1

                    if (check) {
                        correct += 1
                    } else {
                        incorrect += 1
                        check = true  // Reset check flag for the next repetition
                    }
                }
            }

            // Check if the exercise is in the bent position
            if (angleRightHip < 85) {
                stage = 1

                if (check && angleRightHip <= 60) {
                    check = false  // Set check to false to indicate incorrect form
                    addWarning("Do not bend your spine")
                    drawPointsAndLines(canvas, leftHipPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
                    drawPointsAndLines(canvas, leftKneePoints, landmarkPaint, linePaint, Color.RED, Color.RED)

                    drawPointsAndLines(canvas, rightHipPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
                    drawPointsAndLines(canvas, rightKneePoints, landmarkPaint, linePaint, Color.RED, Color.RED)

                    if (angleKneeShoulder < 125) {
                        addWarning("Maintain tension at hamstrings")
                    }
                }
            }


            // Optionally, display angles on the screen or log them
            Log.d("OverlayView", "Left Hip Angle: $angleLeftHip")
            Log.d("OverlayView", "Left Knee Angle: $angleLeftKnee")
            Log.d("OverlayView", "Right Hip Angle: $angleRightHip")
            Log.d("OverlayView", "Right Knee Angle: $angleKneeShoulder")

            displayWarnings(canvas)

            val textPaintLeft = Paint().apply {
                color = Color.WHITE
                textSize = 40f // Adjust text size as needed
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.LEFT
            }

            val textPaintAngles = Paint().apply {
                color = Color.WHITE
                textSize = 40f // Adjust text size as needed
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }

            // Calculate X coordinate for center alignment
            val centerX = canvas.width / 2f

            // Calculate Y coordinates for the text positions
            val baseY = canvas.height - 50f // Start position from the bottom
            val textSpacing = 50f // Space between lines

            // Format angles to one decimal place
            val angleLeftHipFormatted = String.format("%.1f", angleLeftHip)
            val angleLeftKneeFormatted = String.format("%.1f", angleLeftKnee)
            val angleRightHipFormatted = String.format("%.1f", angleRightHip)
            val angleKneeShoulderFormatted = String.format("%.1f", angleKneeShoulder)

            // Draw angles on screen
            canvas.drawText("Left Hip Angle: $angleLeftHipFormatted", centerX, baseY, textPaintAngles)
            canvas.drawText("Left Knee Angle: $angleLeftKneeFormatted", centerX, baseY - textSpacing, textPaintAngles)
            canvas.drawText("Right Hip Angle: $angleRightHipFormatted", centerX, baseY - 2 * textSpacing, textPaintAngles)
            canvas.drawText("Right Knee Angle: $angleKneeShoulderFormatted", centerX, baseY - 3 * textSpacing, textPaintAngles)

            // Draw counters on the screen
            canvas.drawText("",canvas.width - 10f, 100f, textPaintLeft)
            canvas.drawText("Total Reps: $count", 10f, 100f, textPaintLeft) // Adjust x, y values as needed for layout
            canvas.drawText("Correct Reps: $correct", 10f, 150f, textPaintLeft)
            canvas.drawText("Incorrect Reps: $incorrect", 10f, 200f, textPaintLeft)


        } else{drawCommonLandmarks(canvas, result)}

    }

    private fun drawPointsAndLines(canvas: Canvas, points: List<Pair<Float, Float>>, pointPaint: Paint, linePaint: Paint, pointColor: Int, lineColor: Int) {
        points.forEach { point ->
            pointPaint.color = pointColor
            canvas.drawCircle(point.first, point.second, 10f, pointPaint)
        }

        linePaint.color = lineColor
        for (i in 0 until points.size - 1) {
            canvas.drawLine(points[i].first, points[i].second, points[i + 1].first, points[i + 1].second, linePaint)
        }

        // Optionally, log the colors used
        Log.d("OverlayView", "Drawing with Point Color: ${getColorName(pointColor)} and Line Color: ${getColorName(lineColor)}")
    }

    // Helper function to convert color int to a human-readable name (optional)
    private fun getColorName(color: Int): String {
        return when (color) {
            Color.RED -> "Red"
            Color.GREEN -> "Green"
            Color.WHITE -> "White"
            else -> "Custom Color: $color"
        }
    }

    private fun drawCommonLandmarks(canvas: Canvas, result: PoseLandmarkerResult) {
        // This function handles the drawing of common points and lines
        for (landmark in result.landmarks()) {
            for (normalizedLandmark in landmark) {
                canvas.drawPoint(
                    normalizedLandmark.x() * imageWidth * scaleFactor,
                    normalizedLandmark.y() * imageHeight * scaleFactor,
                    pointPaint
                )
            }

            PoseLandmarker.POSE_LANDMARKS.forEach {
                canvas.drawLine(
                    result.landmarks().get(0).get(it!!.start()).x() * imageWidth * scaleFactor,
                    result.landmarks().get(0).get(it.start()).y() * imageHeight * scaleFactor,
                    result.landmarks().get(0).get(it.end()).x() * imageWidth * scaleFactor,
                    result.landmarks().get(0).get(it.end()).y() * imageHeight * scaleFactor,
                    linePaint)
            }
        }
    }

    private fun addWarning(warning: String) {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastWarningTimes[warning] ?: 0  // Get the last time this warning was issued, or 0 if never

        // Add the warning to be displayed on screen immediately
        warnings.add(warning)

        // Speak the warning if it has been at least 5 seconds since last spoken
        if (currentTime - lastTime > 5000) {
            lastWarningTimes[warning] = currentTime  // Update the last time this warning was issued

            // Speak the warning and update the active warnings set
            tts?.speak(warning, TextToSpeech.QUEUE_ADD, null, null)
        }
    }

    private fun displayWarnings(canvas: Canvas) {
        val textPaint = Paint().apply {
            color = Color.YELLOW
            textSize = 50f
            textAlign = Paint.Align.CENTER
        }

        val yOffsetStart = 100f
        var yOffset = yOffsetStart
        warnings.forEach { warning ->
            // Draw warnings on the screen
            canvas.drawText(warning, canvas.width / 2f, yOffset, textPaint)
            yOffset += 50f
        }

        // Clear the list of warnings after drawing to prepare for the next frame
        warnings.clear()
    }

    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int
    ) {
        results = poseLandmarkerResults

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        // PreviewView is in FILL_START mode. So we need to scale up the
        // landmarks to match with the size that the captured images will be
        // displayed.
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        tts?.stop()
        tts?.shutdown()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 12F
    }
}
