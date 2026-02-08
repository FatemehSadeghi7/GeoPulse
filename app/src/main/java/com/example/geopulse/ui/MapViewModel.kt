package com.example.geopulse.ui

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.location.service.GeoLocationService
import com.example.location.service.GeoLocationServiceConnector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val app: Application
) : ViewModel() {

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state

    private var bindJob: Job? = null
    private var collectJob: Job? = null

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(hasLocationPermission = granted) }
        if (granted) bindAndCollect()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startService() {
        GeoLocationService.start(app)
        _state.update { it.copy(isServiceRunning = true) }
        bindAndCollect()
    }

    fun stopService() {
        GeoLocationService.stop(app)
        _state.update { it.copy(isServiceRunning = false) }
        collectJob?.cancel()
        bindJob?.cancel()
    }

    /**
     * پاک کردن مسیر (اختیاری — مثلاً وقتی کاربر tracking جدید شروع میکنه)
     */
    fun clearPath() {
        _state.update { it.copy(pathPoints = emptyList()) }
    }

    private fun bindAndCollect() {
        if (!_state.value.hasLocationPermission) return

        bindJob?.cancel()
        bindJob = viewModelScope.launch {
            GeoLocationServiceConnector.bind(app).collect { service ->
                collectJob?.cancel()
                if (service != null) {
                    collectJob = viewModelScope.launch {
                        service.movingLocations.collect { point ->
                            _state.update {
                                it.copy(
                                    lastPoint = point,
                                    pathPoints = it.pathPoints + point  // ← اضافه کردن به مسیر
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
