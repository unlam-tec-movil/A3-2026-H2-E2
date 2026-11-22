package com.trazo.app.data.repository

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.domain.model.CameraPermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class PermissionRepositoryImplTest {
    private val application: Application = ApplicationProvider.getApplicationContext()
    private lateinit var repository: PermissionRepositoryImpl

    @Before
    fun setUp() {
        repository = PermissionRepositoryImpl(application)
    }

    @Test
    fun `readCameraPermissionStatus returns GRANTED when camera permission is granted`() {
        shadowOf(application).grantPermissions(Manifest.permission.CAMERA)

        val status = repository.readCameraPermissionStatus()

        assertEquals(CameraPermissionStatus.GRANTED, status)
    }

    @Test
    fun `readCameraPermissionStatus returns DENIED when camera permission is denied`() {
        shadowOf(application).denyPermissions(Manifest.permission.CAMERA)

        val status = repository.readCameraPermissionStatus()

        assertEquals(CameraPermissionStatus.DENIED, status)
    }
}
