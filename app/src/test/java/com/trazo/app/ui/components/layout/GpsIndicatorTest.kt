package com.trazo.app.ui.components.layout

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.R
import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GpsIndicatorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun assertQualityIsAnnounced(
        quality: GpsQuality,
        @StringRes label: Int,
    ) {
        composeTestRule.setContent { TrazoTheme { GpsIndicator(gpsQuality = quality) } }

        composeTestRule.onNodeWithContentDescription(context.getString(label)).assertIsDisplayed()
    }

    @Test
    fun `given full signal, when the indicator is shown, then announces full signal`() {
        // Given / When / Then
        assertQualityIsAnnounced(GpsQuality.FULL_SIGNAL, R.string.map_gps_full_signal)
    }

    @Test
    fun `given weak signal, when the indicator is shown, then announces weak signal`() {
        // Given / When / Then
        assertQualityIsAnnounced(GpsQuality.WEAK_SIGNAL, R.string.map_gps_weak_signal)
    }

    @Test
    fun `given no signal, when the indicator is shown, then announces no signal`() {
        // Given / When / Then
        assertQualityIsAnnounced(GpsQuality.NO_SIGNAL, R.string.map_gps_no_signal)
    }
}
