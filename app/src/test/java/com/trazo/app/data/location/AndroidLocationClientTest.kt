package com.trazo.app.data.location

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class AndroidLocationClientTest {
    private val application: Application = ApplicationProvider.getApplicationContext()

    private fun denyLocationPermissions() {
        shadowOf(application).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }

    @Test
    fun `given no location permission, when collecting location updates, then fails with a security exception`() {
        // Given
        denyLocationPermissions()
        val client = AndroidLocationClient(application)

        // When
        val failure = runCatching { runBlocking { client.getLocationUpdates(1_000L).first() } }.exceptionOrNull()

        // Then
        assertTrue(failure is SecurityException)
    }

    @Test
    fun `given no location permission, when collecting signal quality, then fails with a security exception`() {
        // Given
        denyLocationPermissions()
        val client = AndroidLocationClient(application)

        // When
        val failure = runCatching { runBlocking { client.getSignalQualityUpdates().first() } }.exceptionOrNull()

        // Then
        assertTrue(failure is SecurityException)
    }
}
