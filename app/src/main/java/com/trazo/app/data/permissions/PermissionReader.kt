package com.trazo.app.data.permissions

import com.trazo.app.domain.model.CameraPermissionStatus
import com.trazo.app.domain.model.LocationPermissionState

interface PermissionReader {
    fun readLocationPermissionState(): LocationPermissionState

    fun readCameraPermissionStatus(): CameraPermissionStatus
}
