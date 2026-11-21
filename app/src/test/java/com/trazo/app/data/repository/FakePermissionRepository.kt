package com.trazo.app.data.repository

import com.trazo.app.domain.model.CameraPermissionStatus
import com.trazo.app.domain.model.LocationPermissionState

class FakePermissionRepository(
    var locationState: LocationPermissionState = LocationPermissionState.EXACT,
    var cameraStatus: CameraPermissionStatus = CameraPermissionStatus.GRANTED,
) : PermissionRepository {
    var readLocationCalls = 0
    var readCameraCalls = 0

    override fun readLocationPermissionState(): LocationPermissionState {
        readLocationCalls++
        return locationState
    }

    override fun readCameraPermissionStatus(): CameraPermissionStatus {
        readCameraCalls++
        return cameraStatus
    }
}
