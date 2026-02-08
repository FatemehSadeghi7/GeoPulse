package com.example.location.domain.repository


import com.example.location.domain.entity.GeoPoint
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeMovingLocations(): Flow<GeoPoint>
}
