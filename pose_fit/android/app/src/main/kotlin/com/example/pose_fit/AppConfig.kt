package com.example.pose_fit

object AppConfig {
    var selectedModel: Int = 0;
    var isGPUDelagate: Boolean = false;
    var detectionThreshold: Float = 0.5f;
    var trackingThreshold: Float = 0.5f;
    var presenceThreshold: Float = 0.5f;
    var switchCamera: Boolean = false;
}
