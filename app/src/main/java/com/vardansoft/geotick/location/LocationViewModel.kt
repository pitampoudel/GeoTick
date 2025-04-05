package com.vardansoft.geotick.location

import android.app.Application
import android.location.Location
import android.os.Looper
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
    fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        startPredictionLoop()
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }


    private fun startPredictionLoop() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val currentTime = System.currentTimeMillis()
                val elapsedSeconds = (currentTime - lastUpdateTime) / 1000.0
                val lastLocation = lastKnownLocation ?: continue
                if (elapsedSeconds > 3) {
                    val predicted = predictLocation(lastLocation, elapsedSeconds)
                    updateLocation(predicted, predicted = true)
                }
            }
        }
    }

    private fun predictLocation(location: Location, elapsedSeconds: Double): Location {
        var speed = location.speed // m/s
        val bearingRad = Math.toRadians(location.bearing.toDouble())

        val distance = speed * elapsedSeconds // in meters
        val earthRadius = 6371000.0 // meters

        val lat1 = Math.toRadians(location.latitude)
        val lon1 = Math.toRadians(location.longitude)

        val lat2 = asin(
            sin(lat1) * cos(distance / earthRadius) +
                    cos(lat1) * sin(distance / earthRadius) * cos(bearingRad)
        )
        val lon2 = lon1 + atan2(
            sin(bearingRad) * sin(distance / earthRadius) * cos(lat1),
            cos(distance / earthRadius) - sin(lat1) * sin(lat2)
        )

        val newLocation = Location(location.provider).apply {
            latitude = Math.toDegrees(lat2)
            longitude = Math.toDegrees(lon2)
            speed = location.speed
        }

        return newLocation
    }
}
