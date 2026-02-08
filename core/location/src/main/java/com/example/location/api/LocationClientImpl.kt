package com.example.location.api

import com.example.location.domain.entity.GeoPoint
import com.example.location.domain.usecase.ObserveMovingLocationUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocationClientImpl @Inject constructor(
    private val observeMovingLocation: ObserveMovingLocationUseCase
) : LocationClient {
    override fun movingLocations(): Flow<GeoPoint> = observeMovingLocation()
}
