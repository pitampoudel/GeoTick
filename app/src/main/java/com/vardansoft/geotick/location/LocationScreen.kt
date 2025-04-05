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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

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
                    .padding(8.dp)
            ) {
                LocationMapView(
                    latitude = state.latitude,
                    longitude = state.longitude
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
fun LocationMapView(latitude: Double, longitude: Double) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(latitude, longitude), 16f)
    }

    // Smoothly animate camera to new location
    LaunchedEffect(latitude, longitude) {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLng(LatLng(latitude, longitude)),
            durationMs = 1000
        )
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = MarkerState(position = LatLng(latitude, longitude)),
            title = "You are here"
        )
    }
}


@Composable
fun LocationDataDisplay(state: LocationState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
    val speed: Double = 0.0,
    val error: String? = null
)
