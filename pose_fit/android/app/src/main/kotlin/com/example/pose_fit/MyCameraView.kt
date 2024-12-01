package com.example.pose_fit

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.camera.view.PreviewView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import android.annotation.SuppressLint
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Camera
import androidx.camera.core.AspectRatio
import androidx.camera.lifecycle.ProcessCameraProvider
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class MyCameraView(
    private val context: Context,
    messenger: BinaryMessenger,
    id: Int,
    creationParams: Map<String?, Any?>?,
    private val activity: FlutterActivity
) : PlatformView, PoseLandmarkerHelper.LandmarkerListener {

    companion object{
        private const val TAG = "Pose Landmarker"
    }

    private var layout: CoordinatorLayout = LayoutInflater.from(context).inflate(R.layout.camera_view_layout, null) as CoordinatorLayout
    private var previewView: PreviewView = layout.findViewById(R.id.previewView)
    private var overlayView: OverlayView =layout.findViewById(R.id.overlayView)
    private var workoutType: String = creationParams?.get("WorkoutType") as? String ?: "Full_Body"
    private var isTimerRunning = false
    private var isReset =false
    // Camera and Executor
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_BACK
    private var backgroundExecutor: ExecutorService
    private val methodChannel = MethodChannel(messenger, "com.example.pose_fit/configuration")
    private var isCameraActive: Boolean = true

    private var isFrontCamera: Boolean = false


    init {
        backgroundExecutor = Executors.newSingleThreadExecutor()
        setUpCamera()
        backgroundExecutor.execute{
            poseLandmarkerHelper = PoseLandmarkerHelper(
                context = context,
                poseLandmarkerHelperListener = this
            )
            poseLandmarkerHelper.setupPoseLandmarker(
                AppConfig.isGPUDelagate,
                AppConfig.selectedModel,
                AppConfig.detectionThreshold,
                AppConfig.trackingThreshold,
                AppConfig.presenceThreshold
            )
        }
        updateWorkoutTypeText(workoutType)
        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "setTimerRunning" -> {
                    isTimerRunning = call.argument<Boolean>("isRunning") ?: false
                }
                "resetEvent"->{
                    isReset = call.argument<Boolean>("isResetOptionClicked") ?: false
                    overlayView.setResetState(isReset)
                }
                "stopCamera" -> {
                    stopCamera()
                    result.success(null)  // Notify Flutter that the camera has been stopped
                }
                "switchCamera" -> {
                    switchCamera()
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun updateWorkoutTypeText(workoutType: String) {
        val tvConfigValues: TextView = layout.findViewById(R.id.tvConfigValues)
        activity.runOnUiThread {
            tvConfigValues.text = workoutType
            overlayView.setWorkoutType(workoutType)
        }
    }

    private fun switchCamera() {
        isFrontCamera = !isFrontCamera
        cameraFacing = if (isFrontCamera) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindCameraUseCases()
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(previewView.display.rotation)
            .build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(previewView.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build().also {
                it.setAnalyzer(backgroundExecutor) { image ->
                    if (!isCameraActive) {
                        image.close()
                        return@setAnalyzer  // Just return without further processing
                    }
                    detectPose(image)
                }
            }

        cameraProvider.unbindAll()
        try {
            camera = cameraProvider.bindToLifecycle(
                activity, cameraSelector, preview, imageAnalyzer
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectPose(imageProxy: ImageProxy) {
        if (this::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = isFrontCamera
            )
        }
    }

    override fun getView(): View {
        return layout
    }

    override fun dispose() {
        stopCamera()
        backgroundExecutor.shutdownNow()
        try {
            backgroundExecutor.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread{
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            if(errorCode == PoseLandmarkerHelper.GPU_ERROR){
                Log.e(TAG, "Set Delegate to CPU: $error")
            }
            else{
                Log.e(TAG, "Error in MyCameraView: $error")
            }
        }
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        // Handle the results here
        activity.runOnUiThread{
            // Show inference Time
            overlayView.setResults(
                resultBundle.results.first(),
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth
            )
            val tvConfigValues: TextView = layout.findViewById(R.id.tvConfigValues)
            tvConfigValues.text = "Inference Time: ${resultBundle.inferenceTime} ms"
            overlayView.setTimerState(isTimerRunning)
            if(isTimerRunning){
                methodChannel.invokeMethod("sendResultData", mapOf("data" to resultBundle.results.first().toString()))
            }
        }
    }

    // Inside your PlatformView or activity where the camera lifecycle is managed
    private fun stopCamera() {
        isCameraActive = false
        cameraProvider?.unbindAll()
        poseLandmarkerHelper.clearPoseLandmarker()
        backgroundExecutor.shutdownNow()
        try {
            backgroundExecutor.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

}

