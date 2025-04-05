package com.vardansoft.geotick.location

import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.vardansoft.geotick.utils.GpsUtils.checkAndPromptEnableGPS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@HiltViewModel
class LocationViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val locationRequest =
        LocationRequest.Builder(1000).setPriority(Priority.PRIORITY_HIGH_ACCURACY).build()


    private val _isTrackingEnabled = mutableStateOf(false)
    val isTrackingEnabled: State<Boolean> = _isTrackingEnabled

    private var lastKnownLocation: Location? = null
    private var lastUpdateTime: Long = System.currentTimeMillis()
    private val _locationState = mutableStateOf(LocationState())
    val locationState: State<LocationState> = _locationState

    fun updateLocation(location: Location, predicted: Boolean) {
        lastKnownLocation = location
        lastUpdateTime = System.currentTimeMillis()
        _locationState.value = LocationState(
            latitude = location.latitude,
            longitude = location.longitude,
            bearing = location.bearing,
            speed = location.speed.toDouble(),
            error = if (predicted) "Using predicted location" else null
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            updateLocation(location, false)

        }
    }

    @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
    fun startTracking(ctx: Context) {
        _isTrackingEnabled.value = true
        checkAndPromptEnableGPS(
            context = ctx,
            onGPSReady = {

            }
        )
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        startPredictionLoop()
    }

    private fun stopTracking() {
        _isTrackingEnabled.value = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
    fun toggleTracking(ctx: Context) {
        if (_isTrackingEnabled.value) {
            stopTracking()
        } else {
            startTracking(ctx)
        }
    }


    private fun startPredictionLoop() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val currentTime = System.currentTimeMillis()
                val elapsedSeconds = (currentTime - lastUpdateTime) / 1000.0
                val lastKnownLocation = lastKnownLocation ?: continue
                if (elapsedSeconds > 3) {
                    updateLocation(
                        predictLocation(lastKnownLocation, elapsedSeconds),
                        predicted = true
                    )
                }
            }
        }
    }

    private fun predictLocation(lastKnownLocation: Location, elapsedSeconds: Double): Location {
        Log.d("predict", "predicting location")
        var speed = lastKnownLocation.speed // m/s
        val bearingRad = Math.toRadians(lastKnownLocation.bearing.toDouble())

        val distance = speed * elapsedSeconds // in meters
        val earthRadius = 6371000.0 // meters

        val lat1 = Math.toRadians(lastKnownLocation.latitude)
        val lon1 = Math.toRadians(lastKnownLocation.longitude)

        val lat2 = asin(
            sin(lat1) * cos(distance / earthRadius) +
                    cos(lat1) * sin(distance / earthRadius) * cos(bearingRad)
        )
        val lon2 = lon1 + atan2(
            sin(bearingRad) * sin(distance / earthRadius) * cos(lat1),
            cos(distance / earthRadius) - sin(lat1) * sin(lat2)
        )

        return Location(lastKnownLocation.provider).apply {
            latitude = Math.toDegrees(lat2)
            longitude = Math.toDegrees(lon2)
            this.speed = lastKnownLocation.speed
            bearing = lastKnownLocation.bearing
            time = System.currentTimeMillis()
        }
    }

}
