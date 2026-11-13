package com.trazo.app.data.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.trazo.app.domain.model.LocationStatus

/** Le pregunta a Android en qué estado está la ubicación del dispositivo. */
fun readLocationStatus(context: Context): LocationStatus {
    val hasFine = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    val hasCoarse = context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    return when {
        hasFine ->
            if (context.isDeviceLocationEnabled()) {
                LocationStatus.READY
            } else {
                LocationStatus.DEVICE_LOCATION_OFF
            }
        hasCoarse -> LocationStatus.APPROXIMATE_ONLY
        else -> LocationStatus.PERMISSION_DENIED
    }
}

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.isDeviceLocationEnabled(): Boolean {
    val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(manager)
}

/** Arma el Intent que abre la Configuración que corresponde a cada problema. */
fun locationSettingsIntent(
    context: Context,
    status: LocationStatus,
): Intent =
    if (status == LocationStatus.DEVICE_LOCATION_OFF) {
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    } else {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
    }
