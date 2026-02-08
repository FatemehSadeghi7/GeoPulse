package com.example.location.domain.repository


import com.example.location.domain.entity.GeoPoint
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    /** Emits ONLY when motion is detected (stationary updates are suppressed). */
    fun observeMovingLocations(): Flow<GeoPoint>
}
