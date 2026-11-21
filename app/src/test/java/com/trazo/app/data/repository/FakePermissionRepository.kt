package com.trazo.app.data.repository

import com.trazo.app.domain.model.CameraPermissionStatus

class FakePermissionRepository(
    var cameraStatus: CameraPermissionStatus = CameraPermissionStatus.GRANTED,
) : PermissionRepository {
    var readCameraCalls = 0

    override fun readCameraPermissionStatus(): CameraPermissionStatus {
        readCameraCalls++
        return cameraStatus
    }
}
