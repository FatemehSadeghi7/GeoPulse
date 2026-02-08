package com.example.location.data.dataSource


import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.example.location.domain.entity.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject


class GpsLocationDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : RawLocationDataSource {

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    override fun locations(): Flow<GeoPoint> = callbackFlow {
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toGeoPoint())
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        // اگر GPS خام می‌خوایم: فقط GPS_PROVIDER
        val provider = LocationManager.GPS_PROVIDER

        // پارامترها (خام):
        // minTimeMs: حداقل فاصله زمانی بین آپدیت‌ها
        // minDistanceM: حداقل جابجایی برای ارسال آپدیت از سمت سیستم
        val minTimeMs = 1000L
        val minDistanceM = 5f

        locationManager.requestLocationUpdates(
            provider,
            minTimeMs,
            minDistanceM,
            listener
        )

        awaitClose {
            locationManager.removeUpdates(listener)
        }
    }

    private fun Location.toGeoPoint(): GeoPoint = GeoPoint(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        speedMps = if (hasSpeed()) speed else null,
        timeMillis = time
    )
}
