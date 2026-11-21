package com.trazo.app.data.repository

import com.trazo.app.domain.model.CameraPermissionStatus
import com.trazo.app.domain.model.LocationPermissionState

interface PermissionRepository {
    fun readLocationPermissionState(): LocationPermissionState

    fun readCameraPermissionStatus(): CameraPermissionStatus
}
