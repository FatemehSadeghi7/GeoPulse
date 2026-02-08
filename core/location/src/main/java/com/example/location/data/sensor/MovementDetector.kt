package com.example.location.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.sqrt

class MovementDetector(
    private val context: Context
) {
    companion object {
        private const val TAG = "MovementDetector"

        private const val MOVE_THRESHOLD = 0.35f

        private const val MOVE_COUNT_TRIGGER = 5

        private const val STILL_TIMEOUT_MS = 4000L
    }

    fun observeMovement(): Flow<Boolean> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            Log.w(TAG, "No accelerometer!")
            trySend(true)
            awaitClose {}
            return@callbackFlow
        }

        var moveCount = 0
        var lastMovementTime = System.currentTimeMillis()
        var isMoving = false
        var gravX = 0f
        var gravY = 0f
        var gravZ = 0f
        var initialized = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val alpha = 0.8f

                if (!initialized) {
                    gravX = event.values[0]
                    gravY = event.values[1]
                    gravZ = event.values[2]
                    initialized = true
                    return
                }

                gravX = alpha * gravX + (1 - alpha) * event.values[0]
                gravY = alpha * gravY + (1 - alpha) * event.values[1]
                gravZ = alpha * gravZ + (1 - alpha) * event.values[2]

                val lx = event.values[0] - gravX
                val ly = event.values[1] - gravY
                val lz = event.values[2] - gravZ
                val mag = sqrt(lx * lx + ly * ly + lz * lz)

                val now = System.currentTimeMillis()

                if (mag > MOVE_THRESHOLD) {
                    moveCount++
                    if (moveCount >= MOVE_COUNT_TRIGGER) {
                        lastMovementTime = now
                        if (!isMoving) {
                            isMoving = true
                            Log.d(TAG, "→ MOVING (mag=${"%.3f".format(mag)})")
                            trySend(true)
                        }
                    }
                } else {
                    moveCount = 0
                    if (isMoving && (now - lastMovementTime > STILL_TIMEOUT_MS)) {
                        isMoving = false
                        Log.d(TAG, "→ STATIONARY")
                        trySend(false)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        Log.d(TAG, "Sensor registered")

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }.distinctUntilChanged()
}