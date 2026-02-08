package com.example.geopulse.ui

import android.Manifest
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.location.service.GeoLocationService
import com.example.location.service.GeoLocationServiceConnector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val app: Application
) : ViewModel() {

    companion object {
        private const val TAG = "MapViewModel"
    }

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state

    private var bindJob: Job? = null
    private var collectJob: Job? = null
    private var monitorJob: Job? = null

    private var userWantsTracking = false

    private val gpsReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "★ GPS broadcast")
            refreshState()
        }
    }

    init {
        try {
            val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                app.registerReceiver(gpsReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                app.registerReceiver(gpsReceiver, filter)
            }
        } catch (_: Exception) {
        }

        startMonitoring()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            app.unregisterReceiver(gpsReceiver)
        } catch (_: Exception) {
        }
        monitorJob?.cancel()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun refreshState() {
        val hasPerm = hasPermission()
        val gpsOn = isGpsOn()

        _state.update { it.copy(hasLocationPermission = hasPerm, isGpsEnabled = gpsOn) }

        if (userWantsTracking) {
            if (hasPerm && gpsOn) {
                if (!_state.value.isServiceRunning) {
                    resumeService()
                }
            } else {
                if (_state.value.isServiceRunning) {
                    pauseService()
                }
            }
        }
    }


    private fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            app,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            app,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(hasLocationPermission = granted) }
        if (granted) {
            bindAndCollect()
            refreshState()
        }
    }


    private fun isGpsOn(): Boolean {
        val lm = app.getSystemService(LocationManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun startTracking() {
        if (!_state.value.hasLocationPermission) return

        userWantsTracking = true

        if (isGpsOn()) {
            GeoLocationService.start(app)
            _state.update { it.copy(isServiceRunning = true, isGpsEnabled = true) }
            bindAndCollect()
            startMonitoring()
        } else {
            _state.update { it.copy(isGpsEnabled = false) }
        }
    }

    fun stopTracking() {
        userWantsTracking = false
        GeoLocationService.stop(app)
        _state.update { it.copy(isServiceRunning = false) }
        collectJob?.cancel()
        bindJob?.cancel()
        monitorJob?.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun pauseService() {
        Log.d(TAG, "★ Pause: GPS off")
        GeoLocationService.stop(app)
        _state.update { it.copy(isServiceRunning = false) }
        collectJob?.cancel()
        bindJob?.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun resumeService() {
        Log.d(TAG, "★ Resume: GPS on")
        GeoLocationService.start(app)
        _state.update { it.copy(isServiceRunning = true) }
        bindAndCollect()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                refreshState()
            }
        }
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
                                    pathPoints = it.pathPoints + point
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}