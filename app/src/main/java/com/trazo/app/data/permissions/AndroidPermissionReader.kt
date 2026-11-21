package com.trazo.app.data.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.trazo.app.domain.model.CameraPermissionStatus
import com.trazo.app.domain.model.LocationPermissionState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidPermissionReader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : PermissionReader {
        override fun readLocationPermissionState(): LocationPermissionState {
            val hasFine = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            val hasCoarse = context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

            return when {
                hasFine -> LocationPermissionState.EXACT
                hasCoarse -> LocationPermissionState.APPROXIMATE
                else -> LocationPermissionState.DENIED
            }
        }

        override fun readCameraPermissionStatus(): CameraPermissionStatus {
            val hasCamera = context.hasPermission(Manifest.permission.CAMERA)
            return if (hasCamera) CameraPermissionStatus.GRANTED else CameraPermissionStatus.DENIED
        }

        private fun Context.hasPermission(permission: String): Boolean =
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
