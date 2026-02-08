package com.example.geopulse.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

private val Blue = Color(0xFF1976D2)
private val LightBlue = Color(0x441976D2)
private val Green = Color(0xFF43A047)
private val Red = Color(0xFFE53935)
private val Amber = Color(0xFF5C6BC0) // ایندیگو ملایم به جای نارنجی
private val TextPrimary = Color(0xFF212121)
private val TextSecondary = Color(0xFF757575)
private val TextHint = Color(0xFFBDBDBD)
private val DividerColor = Color(0xFFEEEEEE)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MapScreen(vm: MapViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        vm.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        vm.onPermissionResult(fine || coarse)
    }

    val last = state.lastPoint
    val lastLatLng = last?.let { LatLng(it.latitude, it.longitude) }
    val pathLatLngs = state.pathPoints.map { LatLng(it.latitude, it.longitude) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            lastLatLng ?: LatLng(35.6892, 51.3890), 15f
        )
    }

    LaunchedEffect(lastLatLng) {
        if (lastLatLng != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(lastLatLng, 17f), 800
            )
        }
    }

    // پالس
    val inf = rememberInfiniteTransition(label = "p")
    val pRadius by inf.animateFloat(
        1f, 3f,
        infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "pr"
    )
    val pAlpha by inf.animateFloat(
        0.4f, 0f,
        infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "pa"
    )

    Box(Modifier.fillMaxSize()) {

        // ══════ نقشه ══════
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = false),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                mapToolbarEnabled = false
            )
        ) {
            // خط مسیر
            if (pathLatLngs.size >= 2) {
                Polyline(points = pathLatLngs, color = Color(0x30000000), width = 12f)
                Polyline(points = pathLatLngs, color = Blue, width = 7f)
            }
            // نقطه موقعیت
            if (lastLatLng != null) {
                if (state.isServiceRunning) {
                    Circle(
                        center = lastLatLng,
                        radius = (pRadius * 14).toDouble(),
                        fillColor = Blue.copy(alpha = pAlpha),
                        strokeColor = Color.Transparent
                    )
                    Circle(
                        center = lastLatLng,
                        radius = 10.0,
                        fillColor = LightBlue,
                        strokeColor = Blue.copy(alpha = 0.3f),
                        strokeWidth = 1f
                    )
                }
                Circle(
                    center = lastLatLng,
                    radius = 5.0,
                    fillColor = if (state.isServiceRunning) Blue else Color.Gray,
                    strokeColor = Color.White,
                    strokeWidth = 3f
                )
            }
        }

        // ══════ نوار اطلاعات بالا ══════
        AnimatedVisibility(
            visible = state.isServiceRunning && last != null,
            enter = slideInVertically(tween(300)) { -it } + fadeIn(),
            exit = slideOutVertically(tween(200)) { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            if (last != null) {
                val speedKmh = (last.speedMps ?: 0f) * 3.6f
                val accuracy = last.accuracyMeters

                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    // ردیف اول: مختصات کامل
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = Blue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "%.5f, %.5f".format(last.latitude, last.longitude),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    HorizontalDivider(
                        color = DividerColor,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // ردیف دوم: سرعت، دقت، نقاط
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfoItem(
                            title = "Speed",
                            value = "%.1f km/h".format(speedKmh),
                            valueColor = if (speedKmh > 0) Green else TextSecondary
                        )
                        InfoItem(
                            title = "Accuracy",
                            value = if (accuracy != null) "%.0f m".format(accuracy) else "—",
                            valueColor = when {
                                accuracy == null -> TextHint
                                accuracy <= 10f -> Green
                                accuracy <= 25f -> Amber
                                else -> Red
                            }
                        )
                        InfoItem(
                            title = "Points",
                            value = "${state.pathPoints.size}",
                            valueColor = Blue
                        )
                    }
                }
            }
        }

        // ══════ پنل کنترل پایین ══════
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
                .animateContentSize(tween(300)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // نشانگر وضعیت
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    // نقطه وضعیت (با پالس وقتی فعاله)
                    if (state.isServiceRunning) {
                        val statusPulse by inf.animateFloat(
                            0.4f, 1f,
                            infiniteRepeatable(tween(800), RepeatMode.Reverse),
                            label = "sp"
                        )
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Green.copy(alpha = statusPulse))
                        )
                    } else {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(Color.LightGray, CircleShape)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            !state.hasLocationPermission -> "Location Permission Required"
                            state.isServiceRunning -> "Tracking Active"
                            else -> "Ready to Track"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            !state.hasLocationPermission -> Amber
                            state.isServiceRunning -> Green
                            else -> TextSecondary
                        }
                    )
                }

                // دکمه‌ها
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (!state.hasLocationPermission) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                vm.startService()
                            }
                        },
                        enabled = !state.isServiceRunning,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!state.hasLocationPermission) Amber else Blue,
                            contentColor = Color.White,
                            disabledContainerColor = Green.copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 6.dp
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                if (!state.hasLocationPermission)
                                    Icons.Filled.LocationOn else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when {
                                    !state.hasLocationPermission -> "Allow Location"
                                    state.isServiceRunning -> "Running…"
                                    else -> "Start Tracking"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Stop — فقط وقتی سرویس فعاله
                    if (state.isServiceRunning) {
                        OutlinedButton(
                            onClick = { vm.stopService() },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Red
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Stop",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(
    title: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 10.sp,
            color = TextHint,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
