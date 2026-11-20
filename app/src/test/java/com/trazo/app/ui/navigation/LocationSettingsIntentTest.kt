package com.trazo.app.ui.navigation

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.domain.model.LocationStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationSettingsIntentTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

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
