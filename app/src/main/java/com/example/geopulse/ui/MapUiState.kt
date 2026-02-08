package com.example.geopulse.ui

import com.example.location.domain.entity.GeoPoint

data class MapUiState(
    val hasLocationPermission: Boolean = false,
    val lastPoint: GeoPoint? = null,
    val isServiceRunning: Boolean = false
)