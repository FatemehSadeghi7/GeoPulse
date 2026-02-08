package com.example.location.api


import com.example.location.domain.entity.GeoPoint
import kotlinx.coroutines.flow.Flow

interface LocationClient {

    fun movingLocations(): Flow<GeoPoint>
}
