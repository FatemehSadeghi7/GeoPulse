package com.example.location.data.repository

import com.example.location.config.MotionConfig
import com.example.location.data.dataSource.RawLocationDataSource
import com.example.location.data.filter.MotionFilter
import com.example.location.domain.entity.GeoPoint
import com.example.location.domain.repository.LocationRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val dataSource: RawLocationDataSource,
    motionConfig: MotionConfig
) : LocationRepository {

    private val filter = MotionFilter(motionConfig)

    override fun observeMovingLocations(): Flow<GeoPoint> {
        return dataSource.locations()
            .filter { filter.shouldEmit(it) }
    }
}
