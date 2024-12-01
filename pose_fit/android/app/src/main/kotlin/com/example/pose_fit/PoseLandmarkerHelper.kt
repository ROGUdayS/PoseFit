package com.example.pose_fit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class PoseLandmarkerHelper(
    val context: Context,
    // this listener is only used when running in RunningMode.LIVE_STREAM
    val poseLandmarkerHelperListener: LandmarkerListener? = null
) {
    private var poseLandmarker: PoseLandmarker? = null
    private var isCameraActive: Boolean = true

//    init{
//        setupPoseLandmarker(false,0,0.5f,0.5f,0.5f)
//    }

    fun clearPoseLandmarker(){
        poseLandmarker?.close()
        poseLandmarker = null
    }

    fun isClose(): Boolean{
        return poseLandmarker==null
    }

    fun setupPoseLandmarker(
        currentDelegate: Boolean,
        currentModel: Int,
        minPoseDetectionConfidence: Float,
        minPoseTrackingConfidence: Float,
        minPosePresenceConfidence:Float){
        val baseOptionBuilder = BaseOptions.builder()
        when (currentDelegate){
            false -> baseOptionBuilder.setDelegate(Delegate.CPU)
            true -> baseOptionBuilder.setDelegate(Delegate.GPU)
        }
        val selectedModel=
            when(currentModel){
                0 -> "pose_landmarker_lite.task"
                1 -> "pose_landmarker_heavy.task"
                2 -> "pose_landmarker_full.task"
                else -> "pose_landmarker_lite.task"
            }
        baseOptionBuilder.setModelAssetPath(selectedModel)

        // Logging the configuration values
        Log.d(TAG, "Setting up Pose Landmarker with configuration:")
        Log.d(TAG, "Delegate: ${if (currentDelegate) "GPU" else "CPU"}")
        Log.d(TAG, "Model: $selectedModel")
        Log.d(TAG, "Min Pose Detection Confidence: $minPoseDetectionConfidence")
        Log.d(TAG, "Min Tracking Confidence: $minPoseTrackingConfidence")
        Log.d(TAG, "Min Presence Confidence: $minPosePresenceConfidence")
        try {
            val baseOptions = baseOptionBuilder.build()
            // Create an option builder with base options and specific
            // options only use for Pose Landmarker.
            val optionsBuilder =
                PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinPoseDetectionConfidence(minPoseDetectionConfidence)
                    .setMinTrackingConfidence(minPoseTrackingConfidence)
                    .setMinPosePresenceConfidence(minPosePresenceConfidence)
                    .setResultListener(this::returnLivestreamResult)
                    .setErrorListener(this::returnLivestreamError)
                    .setRunningMode(RunningMode.LIVE_STREAM)

            val options = optionsBuilder.build()
            poseLandmarker =
                PoseLandmarker.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            poseLandmarkerHelperListener?.onError(
                "Pose Landmarker failed to initialize. See error logs for " +
                        "details"
            )
            Log.e(
                TAG, "MediaPipe failed to load the task with error: " + e
                    .message
            )
        } catch (e: RuntimeException) {
            // This occurs if the model being used does not support GPU
            poseLandmarkerHelperListener?.onError(
                "Pose Landmarker failed to initialize. See error logs for " +
                        "details", GPU_ERROR
            )
            Log.e(
                TAG,
                "Image classifier failed to load model with error: " + e.message
            )
        }

    }

    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        if (!isCameraActive || poseLandmarker == null) {
            imageProxy.close()
            return
        }
        val frameTime = SystemClock.uptimeMillis()
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            if (isFrontCamera) {
                postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
            }
        }
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true
        )

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        detectAsync(mpImage, frameTime)
    }

    @VisibleForTesting
    fun detectAsync(mpImage: MPImage, frameTime: Long){
        poseLandmarker?.detectAsync(mpImage,frameTime)
    }

    private fun returnLivestreamResult(result: PoseLandmarkerResult, input: MPImage){
        val finishTimeMs=SystemClock.uptimeMillis()
        val inferenceTime=finishTimeMs-result.timestampMs()

        poseLandmarkerHelperListener?.onResults(
            ResultBundle(
                listOf(result),
                inferenceTime,
                input.height,
                input.width
            )
        )
    }

    private fun returnLivestreamError(error: RuntimeException){
        poseLandmarkerHelperListener?.onError(
            error.message ?:"An uknown error has occured"
        )
    }

    data class ResultBundle(
        val results: List<PoseLandmarkerResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int
    )

    companion object{
        const val TAG = "PoseLandmarkerHelper"
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
    }


    interface LandmarkerListener{
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(resultBundle: ResultBundle)
    }
}