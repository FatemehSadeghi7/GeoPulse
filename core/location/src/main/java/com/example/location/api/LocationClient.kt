package com.example.location.api


import com.example.location.domain.entity.GeoPoint
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    /**
     * Stream of user positions.
     * IMPORTANT: Emits ONLY on motion (no stationary jitter).
     */
    fun movingLocations(): Flow<GeoPoint>
}
