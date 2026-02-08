package com.example.location.data.repository

import android.util.Log
import com.example.location.data.dataSource.RawLocationDataSource
import com.example.location.data.sensor.MovementDetector
import com.example.location.domain.entity.GeoPoint
import com.example.location.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val dataSource: RawLocationDataSource,
    private val movementDetector: MovementDetector
) : LocationRepository {

    override fun observeMovingLocations(): Flow<GeoPoint> = channelFlow {
        Log.d("GeoRepo", "★ started")

        val isMoving = MutableStateFlow(false)

        launch {
            movementDetector.observeMovement().collect { moving ->
                Log.d("GeoRepo", "★ accelerometer: $moving")
                isMoving.value = moving
            }
        }

        dataSource.locations().collect { point ->
            if (isMoving.value) {
                Log.d("GeoRepo", "★ EMIT: ${point.latitude},${point.longitude}")
                send(point)
            }
        }
    }
}