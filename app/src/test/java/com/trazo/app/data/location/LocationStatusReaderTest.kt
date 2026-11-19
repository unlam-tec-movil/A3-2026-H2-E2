package com.trazo.app.data.location

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.core.location.LocationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.domain.model.LocationStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class LocationStatusReaderTest {
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val context: Context = application

    private fun setDeviceLocationEnabled(enabled: Boolean) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowOf(manager).setLocationEnabled(enabled)
        assertEquals(enabled, LocationManagerCompat.isLocationEnabled(manager))
    }

    @Test
    fun `given no location permission, when reading the status, then it is permission denied`() {
        // Given
        shadowOf(application).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        // When
        val status = readLocationStatus(context)

        // Then
        assertEquals(LocationStatus.PERMISSION_DENIED, status)
    }

    @Test
    fun `given only coarse permission, when reading the status, then it is approximate only`() {
        // Given
        shadowOf(application).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

        // When
        val status = readLocationStatus(context)

        // Then
        assertEquals(LocationStatus.APPROXIMATE_ONLY, status)
    }

    @Test
    fun `given fine permission and the device location on, when reading the status, then it is ready`() {
        // Given
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        setDeviceLocationEnabled(true)

        // When
        val status = readLocationStatus(context)

        // Then
        assertEquals(LocationStatus.READY, status)
    }

    @Test
    fun `given fine permission and the device location off, when reading the status, then it is device location off`() {
        // Given
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        setDeviceLocationEnabled(false)

        // When
        val status = readLocationStatus(context)

        // Then
        assertEquals(LocationStatus.DEVICE_LOCATION_OFF, status)
    }

    @Test
    fun `given the adapter, when reading, then it delegates to the Android status reader`() {
        // Given
        shadowOf(application).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        // When
        val status = AndroidLocationStatusReader(context).read()

        // Then
        assertEquals(LocationStatus.PERMISSION_DENIED, status)
    }

    @Test
    fun `given the device location is off, when building the settings intent, then opens the location settings`() {
        // Given / When
        val intent = locationSettingsIntent(context, LocationStatus.DEVICE_LOCATION_OFF)

        // Then
        assertEquals(Settings.ACTION_LOCATION_SOURCE_SETTINGS, intent.action)
    }

    @Test
    fun `given a permission problem, when building the settings intent, then opens the app details`() {
        // Given / When
        val intent: Intent = locationSettingsIntent(context, LocationStatus.PERMISSION_DENIED)

        // Then
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:${context.packageName}", intent.dataString)
    }
}
