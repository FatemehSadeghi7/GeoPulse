package com.example.location.data.dataSource


import com.example.location.domain.entity.GeoPoint
import kotlinx.coroutines.flow.Flow

interface RawLocationDataSource {
    /** Emits raw locations from Android providers (no filtering). */
    fun locations(): Flow<GeoPoint>
}
