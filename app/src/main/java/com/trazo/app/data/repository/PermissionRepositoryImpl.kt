package com.trazo.app.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.trazo.app.domain.model.CameraPermissionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRepositoryImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : PermissionRepository {
        override fun readCameraPermissionStatus(): CameraPermissionStatus {
            val hasCamera =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED
            return if (hasCamera) CameraPermissionStatus.GRANTED else CameraPermissionStatus.DENIED
        }
    }
