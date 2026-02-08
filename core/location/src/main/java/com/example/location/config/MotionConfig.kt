package com.example.location.config

data class MotionConfig(
    val maxAccuracyMeters: Float = 100f,
    val minDisplacementMeters: Float = 5f
)