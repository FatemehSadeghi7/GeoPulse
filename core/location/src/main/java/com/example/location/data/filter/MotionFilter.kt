package com.example.location.data.filter

import com.example.location.config.MotionConfig
import com.example.location.domain.entity.GeoPoint

import kotlin.math.*

class MotionFilter(
    private val config: MotionConfig
) {
    private var lastEmitted: GeoPoint? = null

    /**
     * Returns true if this point should be emitted (moving),
     * false if it should be suppressed (stationary/jitter/low quality).
     */
    fun shouldEmit(next: GeoPoint): Boolean {
        // 1) Accuracy gate
        val acc = next.accuracyMeters
        if (acc != null && acc > config.maxAccuracyMeters) return false

        val prev = lastEmitted
        if (prev == null) {
            lastEmitted = next
            return true // first fix
        }

        // 2) Speed gate (if available)
        val speed = next.speedMps
        if (speed != null && speed < config.minSpeedMps) return false

        // 3) Distance gate
        val distance = haversineMeters(
            prev.latitude, prev.longitude,
            next.latitude, next.longitude
        )
        if (distance < config.minDisplacementMeters) return false

        lastEmitted = next
        return true
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
