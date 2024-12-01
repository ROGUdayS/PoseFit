package com.example.pose_fit

import android.Manifest
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import android.widget.Toast
import io.flutter.embedding.engine.FlutterEngine

class MainActivity : FlutterActivity() {
    private val cameraPermissionCode = 101
    private val cameraPermissionChannel = "NativeView"
    private val CHANNEL = "com.example.pose_fit/configuration"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
                call, result ->
            when (call.method) {
                "updateConfig" -> {
                    val selectedModel = call.argument<Int>("selectedModel")
                    val isGPUDelagate = call.argument<Boolean>("isGPUDelagate")
                    val detectionThreshold = call.argument<Double>("detectionThreshold")?.toFloat()
                    val trackingThreshold = call.argument<Double>("trackingThreshold")?.toFloat()
                    val presenceThreshold = call.argument<Double>("presenceThreshold")?.toFloat()
                    val switchCamera = call.argument<Boolean>("switchCamera")

                    // Use these values to configure your native components
                    if (selectedModel != null && isGPUDelagate != null && detectionThreshold != null && trackingThreshold != null && presenceThreshold != null && switchCamera != null) {
                        updateNativeConfiguration(selectedModel, isGPUDelagate, detectionThreshold, trackingThreshold, presenceThreshold, switchCamera)
                    }
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun updateNativeConfiguration(selectedModel: Int, isGPUDelagate: Boolean, detectionThreshold: Float, trackingThreshold: Float, presenceThreshold: Float, switchCamera: Boolean) {
        AppConfig.selectedModel = selectedModel
        AppConfig.isGPUDelagate = isGPUDelagate
        AppConfig.detectionThreshold = detectionThreshold
        AppConfig.trackingThreshold = trackingThreshold
        AppConfig.presenceThreshold = presenceThreshold
        AppConfig.switchCamera = switchCamera
        Toast.makeText(this, "Configuration Updated", Toast.LENGTH_SHORT).show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkCameraPermission()

    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraPermissionCode)
        } else {
            registerPlatformView()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, cameraPermissionChannel).invokeMethod("permissionGranted", null)
                registerPlatformView() // Now correctly passes necessary parameters
            } else {
                MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, cameraPermissionChannel).invokeMethod("navigateToHome", null)
            }
        }
    }

    private fun registerPlatformView() {
        flutterEngine?.platformViewsController?.registry?.registerViewFactory("NativeView", CameraViewFactory(this, flutterEngine!!.dartExecutor.binaryMessenger))
    }
}
