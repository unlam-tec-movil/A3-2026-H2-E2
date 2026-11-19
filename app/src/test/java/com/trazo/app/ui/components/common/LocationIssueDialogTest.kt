package com.trazo.app.ui.components.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.R
import com.trazo.app.domain.model.LocationStatus
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationIssueDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var settingsOpened = 0
    private var dismissals = 0

    private fun showDialog(issue: LocationStatus) {
        composeTestRule.setContent {
            TrazoTheme {
                LocationIssueDialog(
                    issue = issue,
                    onOpenSettings = { settingsOpened++ },
                    onDismiss = { dismissals++ },
                )
            }
        }
    }

    private fun assertTitle(
        issue: LocationStatus,
        @StringRes title: Int,
    ) {
        showDialog(issue)

        composeTestRule.onNodeWithText(context.getString(title)).assertExists()
    }

    @Test
    fun `given a denied permission, when the dialog is shown, then explains the permission is missing`() {
        // Given / When / Then
        assertTitle(LocationStatus.PERMISSION_DENIED, R.string.location_issue_permission_title)
    }

    @Test
    fun `given approximate-only permission, when the dialog is shown, then explains precise location is needed`() {
        // Given / When / Then
        assertTitle(LocationStatus.APPROXIMATE_ONLY, R.string.location_issue_approximate_title)
    }

    @Test
    fun `given device location off, when the dialog is shown, then explains location is turned off`() {
        // Given / When / Then
        assertTitle(LocationStatus.DEVICE_LOCATION_OFF, R.string.location_issue_device_off_title)
    }

    @Test
    fun `given the dialog, when tapping open settings, then notifies the request`() {
        // Given
        showDialog(LocationStatus.PERMISSION_DENIED)

        // When
        composeTestRule.onNodeWithText(context.getString(R.string.location_issue_open_settings)).performClick()

        // Then
        assertEquals(1, settingsOpened)
    }

    @Test
    fun `given the dialog, when closing it, then notifies the dismissal`() {
        // Given
        showDialog(LocationStatus.PERMISSION_DENIED)

        // When
        composeTestRule.onNodeWithText(context.getString(R.string.location_issue_close)).performClick()

        // Then
        assertEquals(1, dismissals)
    }
}
