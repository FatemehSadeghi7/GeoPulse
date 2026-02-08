package com.example.location.service


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

internal object GeoLocationNotification {

    const val CHANNEL_ID = "geovector_location_channel"
    const val CHANNEL_NAME = "Location Tracking"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                )
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun build(context: Context): Notification {
        // اگر آیکن نداشتی، یک آیکن ساده در app یا location module اضافه کن.
        // فعلاً از آیکن پیشفرض سیستم هم میشه استفاده کرد، ولی بهتره آیکن خودت باشه.
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("GeoVector")
            .setContentText("Tracking location while moving…")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
