package com.trazo.app.data.repository

import com.trazo.app.data.permissions.PermissionReader
import com.trazo.app.domain.model.CameraPermissionStatus
import com.trazo.app.domain.model.LocationPermissionState
import javax.inject.Inject

class PermissionRepositoryImpl
    @Inject
    constructor(
        private val permissionReader: PermissionReader,
    ) : PermissionRepository {
        override fun readLocationPermissionState(): LocationPermissionState = permissionReader.readLocationPermissionState()

        override fun readCameraPermissionStatus(): CameraPermissionStatus = permissionReader.readCameraPermissionStatus()
    }
