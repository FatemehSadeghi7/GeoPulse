package com.example.geopulse.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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
private val Amber = Color(0xFF5C6BC0)
private val TextPrimary = Color(0xFF212121)
private val TextSecondary = Color(0xFF757575)

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

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            vm.refreshState()
        }
    }

    val last = state.lastPoint
    val lastLatLng = last?.let { LatLng(it.latitude, it.longitude) }
    val pathLatLngs = state.pathPoints.map { LatLng(it.latitude, it.longitude) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(lastLatLng ?: LatLng(35.6892, 51.3890), 15f)
    }

    LaunchedEffect(lastLatLng) {
        if (lastLatLng != null) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(lastLatLng, 17f), 800)
        }
    }

    val inf = rememberInfiniteTransition(label = "p")
    val pRadius by inf.animateFloat(
        1f,
        3f,
        infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "pr"
    )
    val pAlpha by inf.animateFloat(
        0.4f,
        0f,
        infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "pa"
    )


    state.isServiceRunning.not() && state.hasLocationPermission && !state.isGpsEnabled

    Box(Modifier.fillMaxSize()) {

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
            if (pathLatLngs.size >= 2) {
                Polyline(points = pathLatLngs, color = Color(0x30000000), width = 12f)
                Polyline(points = pathLatLngs, color = Blue, width = 7f)
            }
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

        AnimatedVisibility(
            visible = state.isServiceRunning && last != null,
            enter = slideInVertically(tween(300)) { -it } + fadeIn(),
            exit = slideOutVertically(tween(200)) { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            if (last != null) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LocationOn,
                                null,
                                tint = Blue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "%.5f, %.5f".format(last.latitude, last.longitude),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            "${state.pathPoints.size} pts",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue
                        )
                    }
                }
            }
        }

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

                if (!state.isGpsEnabled && state.hasLocationPermission) {
                    Icon(Icons.Filled.LocationOn, null, tint = Red, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "GPS is turned off",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Red,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Please enable GPS to start tracking",
                        fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                    Button(
                        onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue)
                    ) {
                        Text("Enable GPS", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        if (state.isServiceRunning) {
                            val sp by inf.animateFloat(
                                0.4f,
                                1f,
                                infiniteRepeatable(tween(800), RepeatMode.Reverse),
                                label = "sp"
                            )
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Green.copy(alpha = sp))
                            )
                        } else {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .background(
                                        if (!state.hasLocationPermission) Amber else Color.LightGray,
                                        CircleShape
                                    )
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                !state.hasLocationPermission -> "Location Permission Needed"
                                state.isServiceRunning -> "Tracking Active"
                                else -> "Ready to Track"
                            },
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = when {
                                !state.hasLocationPermission -> Amber
                                state.isServiceRunning -> Green
                                else -> TextSecondary
                            }
                        )
                    }

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
                                    vm.startTracking()
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
                                2.dp,
                                pressedElevation = 6.dp
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    if (!state.hasLocationPermission) Icons.Filled.LocationOn else Icons.Filled.PlayArrow,
                                    null, modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    when {
                                        !state.hasLocationPermission -> "Allow Location"
                                        state.isServiceRunning -> "Running…"
                                        else -> "Start Tracking"
                                    },
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp
                                )
                            }
                        }

                        if (state.isServiceRunning) {
                            OutlinedButton(
                                onClick = { vm.stopTracking() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Stop", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}