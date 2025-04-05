package com.vardansoft.geotick.location

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.vardansoft.geotick.R
import com.vardansoft.geotick.utils.BitmapUtils.bitmapDescriptorFromVector

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationScreen(viewModel: LocationViewModel = hiltViewModel()) {
    val state by viewModel.locationState

    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            viewModel.startLocationUpdates()
        }
    }

    when {
        locationPermissionState.status.isGranted -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                LocationMapView(
                    latitude = state.latitude,
                    longitude = state.longitude,
                    bearing = state.bearing
                )

                Spacer(modifier = Modifier.height(16.dp))

                LocationDataDisplay(state)
            }
        }

        locationPermissionState.status.shouldShowRationale -> {
            PermissionRationale {
                locationPermissionState.launchPermissionRequest()
            }
        }

        else -> {
            PermissionRequestButton {
                locationPermissionState.launchPermissionRequest()
            }
        }
    }
}


@Composable
fun LocationMapView(
    latitude: Double,
    longitude: Double,
    bearing: Float
) {
    val ctx = LocalContext.current
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(latitude, longitude), 16f)
    }

    val userLatLng = LatLng(latitude, longitude)

    // Animate camera to follow user
    LaunchedEffect(latitude, longitude) {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLng(userLatLng),
            durationMs = 1000
        )
    }

    var markerIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

    // Load marker safely AFTER composition starts
    LaunchedEffect(Unit) {
        markerIcon = bitmapDescriptorFromVector(ctx, R.drawable.navigation_arrow)
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            icon = markerIcon,
            state = MarkerState(position = userLatLng),
            title = "You are here",
            rotation = bearing, // Direction the user is heading
            anchor = Offset(0.5f, 0.5f), // Center the rotation
            flat = true // Important: makes rotation match the map orientation
        )
    }
}


@Composable
fun LocationDataDisplay(state: LocationState) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Latitude: ${state.latitude}", style = MaterialTheme.typography.bodySmall)
        Text("Longitude: ${state.longitude}", style = MaterialTheme.typography.bodySmall)
        Text("Speed: ${"%.2f".format(state.speed)} m/s", style = MaterialTheme.typography.bodySmall)

        state.error?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun PermissionRationale(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Location permission is needed to show your current position.")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRequest) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun PermissionRequestButton(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onRequest) {
            Text("Request Location Permission")
        }
    }
}


data class LocationState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val bearing: Float = 0.0F,
    val speed: Double = 0.0,
    val error: String? = null
)
