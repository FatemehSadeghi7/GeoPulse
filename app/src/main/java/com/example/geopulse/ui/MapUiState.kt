package com.example.geopulse.ui

import com.example.location.domain.entity.GeoPoint

data class MapUiState(
    val hasLocationPermission: Boolean = false,
    val isGpsEnabled: Boolean = true,
    val isServiceRunning: Boolean = false,
    val lastPoint: GeoPoint? = null,
    val pathPoints: List<GeoPoint> = emptyList()
)