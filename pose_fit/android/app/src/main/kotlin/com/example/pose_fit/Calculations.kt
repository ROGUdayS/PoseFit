package com.example.pose_fit

import android.graphics.Color
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.PI

class Calculations {
    companion object {
        fun calculateAngles(a: Pair<Float, Float>, b: Pair<Float, Float>, c: Pair<Float, Float>): Double {
            // Calculate direction vectors from point B to points A and C
            val ba = Pair(a.first - b.first, a.second - b.second) // Vector from B to A
            val bc = Pair(c.first - b.first, c.second - b.second) // Vector from B to C

            // Calculate the atan2 of the vectors to get the angle in radians
            val angleRadians = atan2(bc.second, bc.first) - atan2(ba.second, ba.first)

            // Convert radians to degrees and take the absolute value
            var angleDegrees = abs(angleRadians * 180 / PI)

            // Normalize the angle to be within [0, 180] degrees
            if (angleDegrees > 180) {
                angleDegrees = 360 - angleDegrees
            }

            return angleDegrees
        }
    }
}

//if(angleLeftArm in 1.0..22.0 && lStage == "down"){
//    lStage="up"
//    lCounter++
//    if(lIsCorrect){
//        lCorrectCounter++
//        addWarning("Left Arm Down")
//        if (angleLeftArm in 5.0..15.0){drawPointsAndLines(canvas, leftArmPoints, landmarkPaint, linePaint, Color.GREEN, Color.GREEN)}
//    } else{
//        addWarning("Left Arm Not Fully Up")
//        drawPointsAndLines(canvas, leftArmPoints, landmarkPaint, linePaint, Color.YELLOW, Color.YELLOW)
//        lIncorrectCounter++
//    }
//    //                    drawPointsAndLines(canvas, leftArmPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)
//    lIsCorrect = true
//}
//else if (angleLeftArm>150){
//    lStage = "down"
//    addWarning("Left Arm Up")
//    if (angleLeftArm in 165.0..170.0){drawPointsAndLines(canvas, leftArmPoints, landmarkPaint, linePaint, Color.GREEN, Color.GREEN)}
//    else if(angleLeftArm>175 && lIsCorrect && lCounter >1) {
//        addWarning("Lost Tension in Left Arm")
//        drawPointsAndLines(canvas, leftArmPoints, landmarkPaint, linePaint, Color.RED, Color.RED)
//        lIsCorrect =false
//    }
//    else{drawPointsAndLines(canvas, leftArmPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)}
//}
//else{
//    drawPointsAndLines(canvas, leftArmPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)
//    drawPointsAndLines(canvas,leftWristPoints, landmarkPaint, linePaint, Color.WHITE, Color.WHITE)
//}