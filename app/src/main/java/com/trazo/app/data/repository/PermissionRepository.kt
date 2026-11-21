package com.trazo.app.data.repository

import com.trazo.app.domain.model.CameraPermissionStatus

interface PermissionRepository {
    fun readCameraPermissionStatus(): CameraPermissionStatus
}
