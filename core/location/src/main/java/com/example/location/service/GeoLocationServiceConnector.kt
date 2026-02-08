package com.example.location.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object GeoLocationServiceConnector {

    fun bind(context: Context): Flow<GeoLocationService?> = callbackFlow {
        val appContext = context.applicationContext

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as? GeoLocationService.LocalBinder
                trySend(binder?.service())
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                trySend(null)
            }
        }

        val intent = Intent(appContext, GeoLocationService::class.java)
        appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        awaitClose {
            runCatching { appContext.unbindService(connection) }
        }
    }
}
