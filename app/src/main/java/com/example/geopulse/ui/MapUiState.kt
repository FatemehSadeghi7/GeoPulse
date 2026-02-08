package com.example.geopulse.ui

import com.example.location.domain.entity.GeoPoint

data class MapUiState(
    val hasLocationPermission: Boolean = false,
    val isGpsEnabled: Boolean = true,
    val lastPoint: GeoPoint? = null,
    val pathPoints: List<GeoPoint> = emptyList(),
    val isServiceRunning: Boolean = false
)