package com.example.location.config


data class MotionConfig(
    val maxAccuracyMeters: Float = 30f,
    val minDisplacementMeters: Float = 3f,
    val minSpeedMps: Float = 0.3f
)
