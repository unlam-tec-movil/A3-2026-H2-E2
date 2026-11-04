package com.trazo.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.os.SystemClock.elapsedRealtimeNanos
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.RawLocationPoint
import com.trazo.app.domain.repository.LocationClient
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidLocationClient
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : LocationClient {
        private val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(context)

        @SuppressLint("MissingPermission")
        override fun getLocationUpdates(intervalMillis: Long): Flow<RawLocationPoint> =
            callbackFlow {
                if (!hasLocationPermission()) {
                    close(SecurityException("No se concedieron permisos de ubicación."))
                    return@callbackFlow
                }

                val request =
                    LocationRequest
                        .Builder(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            intervalMillis.coerceAtLeast(1_000L),
                        ).setMinUpdateIntervalMillis(
                            (intervalMillis / 2).coerceAtLeast(500L),
                        ).build()

                val callback =
                    object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            result.locations.forEach { location ->
                                trySend(location.toRawLocationPoint())
                            }
                        }
                    }

                fusedLocationClient
                    .requestLocationUpdates(
                        request,
                        callback,
                        Looper.getMainLooper(),
                    ).addOnFailureListener { exception ->
                        close(exception)
                    }

                awaitClose {
                    fusedLocationClient.removeLocationUpdates(callback)
                }
            }

        @SuppressLint("MissingPermission")
        override fun getSignalQualityUpdates(): Flow<GpsQuality> =
            callbackFlow {
                if (!hasLocationPermission()) {
                    close(SecurityException("No se concedieron permisos de ubicación."))
                    return@callbackFlow
                }

                val request =
                    LocationRequest
                        .Builder(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            1_000L,
                        ).build()

                val callback =
                    object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            result.lastLocation?.let { location ->
                                trySend(location.toGpsQuality())
                            }
                        }

                        override fun onLocationAvailability(availability: LocationAvailability) {
                            if (!availability.isLocationAvailable) {
                                trySend(GpsQuality.NO_SIGNAL)
                            }
                        }
                    }

                fusedLocationClient
                    .requestLocationUpdates(
                        request,
                        callback,
                        Looper.getMainLooper(),
                    ).addOnFailureListener { exception ->
                        close(exception)
                    }

                awaitClose {
                    fusedLocationClient.removeLocationUpdates(callback)
                }
            }

        private fun hasLocationPermission(): Boolean {
            val fineLocationGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED

            val coarseLocationGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED

            return fineLocationGranted || coarseLocationGranted
        }

        private fun Location.toRawLocationPoint(): RawLocationPoint =
            RawLocationPoint(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = accuracy,
                speedMetersPerSecond = if (hasSpeed()) speed else 0f,
                monotonicTimestampMillis = elapsedRealtimeNanos() / 1_000_000L,
            )

        private fun Location.toGpsQuality(): GpsQuality =
            when {
                accuracy <= 10f -> GpsQuality.FULL_SIGNAL
                accuracy <= 20f -> GpsQuality.MEDIUM_SIGNAL
                else -> GpsQuality.LOW_SIGNAL
            }
    }
