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

    private val gpsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "★ GPS broadcast received: ${intent.action}")
            checkPermissionsAndGps()
        }
    }

    init {
        // ثبت receiver برای GPS on/off
        try {
            val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                app.registerReceiver(gpsReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                app.registerReceiver(gpsReceiver, filter)
            }
            Log.d(TAG, "★ GPS receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "★ Failed to register GPS receiver", e)
        }

        // شروع مانیتورینگ دائمی
        startMonitoring()
    }

    override fun onCleared() {
        super.onCleared()
        try { app.unregisterReceiver(gpsReceiver) } catch (_: Exception) {}
        monitorJob?.cancel()
    }

    fun checkPermissionsAndGps() {
        val fine = ContextCompat.checkSelfPermission(
            app, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            app, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasPermission = fine || coarse

        val locationManager = app.getSystemService(LocationManager::class.java)
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        val prev = _state.value
        val changed = prev.hasLocationPermission != hasPermission || prev.isGpsEnabled != gpsEnabled

        if (changed) {
            Log.d(TAG, "★ Status changed: permission=$hasPermission gps=$gpsEnabled")
        }

        _state.update {
            it.copy(
                hasLocationPermission = hasPermission,
                isGpsEnabled = gpsEnabled
            )
        }

        // پرمیشن یا GPS نداره و سرویس فعاله → stop
        if ((!hasPermission || !gpsEnabled) && _state.value.isServiceRunning) {
            Log.d(TAG, "★ Stopping service: permission=$hasPermission gps=$gpsEnabled")
            stopService()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(hasLocationPermission = granted) }
        if (granted) {
            checkPermissionsAndGps()
            bindAndCollect()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startService() {
        checkPermissionsAndGps()
        if (!_state.value.hasLocationPermission || !_state.value.isGpsEnabled) return

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

    fun clearPath() {
        _state.update { it.copy(pathPoints = emptyList()) }
    }

    /**
     * همیشه هر ۱ ثانیه چک میکنه — چون:
     * - پرمیشن لوکیشن broadcast نداره
     * - بعضی گوشی‌ها GPS broadcast رو درست نمیفرستن
     */
    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                checkPermissionsAndGps()
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
