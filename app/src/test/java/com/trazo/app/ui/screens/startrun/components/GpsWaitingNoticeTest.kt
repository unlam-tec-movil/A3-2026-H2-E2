package com.trazo.app.ui.screens.startrun.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.R
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GpsWaitingNoticeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var cancellations = 0

    private fun showNotice() {
        composeTestRule.setContent {
            TrazoTheme { GpsWaitingNotice(goalLabel = "5 km", onCancel = { cancellations++ }) }
        }
    }

    @Test
    fun `given the app waits for GPS, when the notice is shown, then displays the waiting message and the goal`() {
        // Given / When
        showNotice()

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.start_run_waiting_gps)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.start_run_waiting_goal, "5 km")).assertIsDisplayed()
    }

    @Test
    fun `given the notice, when tapping cancel, then notifies the cancellation`() {
        // Given
        showNotice()

        // When
        composeTestRule.onNodeWithText(context.getString(R.string.start_run_cancel)).performClick()

        // Then
        assertEquals(1, cancellations)
    }
}
