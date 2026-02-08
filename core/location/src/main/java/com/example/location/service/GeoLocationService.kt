package com.example.location.service


import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.example.location.api.LocationClient
import com.example.location.domain.entity.GeoPoint

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GeoLocationService : Service() {

    companion object {
        const val ACTION_START = "com.example.geopulse.location.START"
        const val ACTION_STOP = "com.example.geovepulse.location.STOP"

        fun start(context: Context) {
            val intent = Intent(context, GeoLocationService::class.java)
                .apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent =
                Intent(context, GeoLocationService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }

    @Inject
    lateinit var locationClient: LocationClient

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main.immediate + serviceJob)

    private val binder = LocalBinder()

    private val _movingLocations = MutableSharedFlow<GeoPoint>(
        replay = 1,
        extraBufferCapacity = 8
    )
    val movingLocations: Flow<GeoPoint> = _movingLocations

    private val sharedFromClient by lazy {
        locationClient.movingLocations()
            .shareIn(
                scope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                replay = 1
            )
    }

    override fun onCreate() {
        super.onCreate()
        GeoLocationNotification.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
            else -> { /* ignore */
            }
        }
        return START_STICKY
    }

    private var isTracking = false

    private fun startTracking() {
        if (isTracking) return
        isTracking = true

        startForeground(
            GeoLocationNotification.NOTIFICATION_ID,
            GeoLocationNotification.build(this)
        )

        scope.launch {
            sharedFromClient.collect { point ->
                _movingLocations.emit(point)
            }
        }
    }

    private fun stopTracking() {
        isTracking = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    inner class LocalBinder : Binder() {
        fun service(): GeoLocationService = this@GeoLocationService
    }
}
