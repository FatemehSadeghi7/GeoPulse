package com.example.location.data.dataSource

import com.example.location.domain.entity.GeoPoint
import kotlinx.coroutines.flow.Flow

interface RawLocationDataSource {
    fun locations(): Flow<GeoPoint>

    fun lastKnownLocation(): GeoPoint?
}