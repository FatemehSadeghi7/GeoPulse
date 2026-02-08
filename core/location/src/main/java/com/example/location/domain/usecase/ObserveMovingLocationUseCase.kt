package com.example.location.domain.usecase



import com.example.location.domain.entity.GeoPoint
import com.example.location.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMovingLocationUseCase @Inject constructor(
    private val repository: LocationRepository
) {
    operator fun invoke(): Flow<GeoPoint> = repository.observeMovingLocations()
}
