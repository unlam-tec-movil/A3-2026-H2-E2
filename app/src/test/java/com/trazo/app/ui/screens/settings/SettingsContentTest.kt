package com.trazo.app.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.domain.model.CameraPermissionStatus
import com.trazo.app.domain.model.LocationStatus
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var backClicks = 0
    private var systemSettingsClicks = 0

    private fun string(
        @StringRes id: Int,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setContent(state: SettingsUiState) {
        composeTestRule.setContent {
            TrazoTheme {
                SettingsContent(
                    state = state,
                    onBackClick = { backClicks++ },
                    onOpenSystemSettingsClick = { systemSettingsClicks++ },
                )
            }
        }
    }

    @Test
    fun readyLocationAndGrantedCamera_showsExactAndGranted_andTheGrantedNoticeWithSettingsButton() {
        setContent(
            SettingsUiState(
                locationStatus = LocationStatus.READY,
                cameraPermissionStatus = CameraPermissionStatus.GRANTED,
            ),
        )

        composeTestRule
            .onNodeWithText(string(R.string.settings_permission_exact))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.settings_permission_granted))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.settings_permission_notice_granted))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.settings_permission_notice_title))
            .assertDoesNotExist()
        composeTestRule
            .onAllNodesWithText(string(R.string.settings_go_to_settings))
            .assertCountEquals(1)
    }

    @Test
    fun allPermissionsGranted_clickGoToSettingsButton_opensTheSystemSettingsToRevoke() {
        setContent(
            SettingsUiState(
                locationStatus = LocationStatus.READY,
                cameraPermissionStatus = CameraPermissionStatus.GRANTED,
            ),
        )

        composeTestRule
            .onNodeWithText(string(R.string.settings_go_to_settings))
            .performScrollTo()
            .performClick()

        assertEquals(1, systemSettingsClicks)
    }

    @Test
    fun deviceLocationOffAndGrantedCamera_showsExactAndGranted_andASingleSettingsButton() {
        setContent(
            SettingsUiState(
                locationStatus = LocationStatus.DEVICE_LOCATION_OFF,
                cameraPermissionStatus = CameraPermissionStatus.GRANTED,
            ),
        )

        composeTestRule
            .onNodeWithText(string(R.string.settings_permission_exact))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.settings_permission_granted))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(string(R.string.settings_go_to_settings))
            .assertCountEquals(1)
    }

    @Test
    fun deniedLocationAndDeniedCamera_showsDenied_theRestrictedNotice_andASingleSettingsButton() {
        setContent(
            SettingsUiState(
                locationStatus = LocationStatus.PERMISSION_DENIED,
                cameraPermissionStatus = CameraPermissionStatus.DENIED,
            ),
        )

        composeTestRule
            .onAllNodesWithText(string(R.string.settings_permission_denied))
            .assertCountEquals(2)
        composeTestRule
            .onNodeWithText(string(R.string.settings_permission_notice_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.settings_permission_notice_body))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.settings_permission_notice_granted))
            .assertDoesNotExist()
        composeTestRule
            .onAllNodesWithText(string(R.string.settings_go_to_settings))
            .assertCountEquals(1)
    }

    @Test
    fun deniedLocationAndUnknownCamera_showsDeniedTwice_andASingleSettingsButton() {
        setContent(
            SettingsUiState(
                locationStatus = LocationStatus.PERMISSION_DENIED,
                cameraPermissionStatus = CameraPermissionStatus.UNKNOWN,
            ),
        )

        composeTestRule
            .onAllNodesWithText(string(R.string.settings_permission_denied))
            .assertCountEquals(2)
        composeTestRule
            .onAllNodesWithText(string(R.string.settings_go_to_settings))
            .assertCountEquals(1)
    }

    @Test
    fun approximateLocationAndGrantedCamera_showsApproximate_andTheRestrictedNotice() {
        setContent(
            SettingsUiState(
                locationStatus = LocationStatus.APPROXIMATE_ONLY,
                cameraPermissionStatus = CameraPermissionStatus.GRANTED,
            ),
        )

        composeTestRule
            .onNodeWithText(string(R.string.settings_permission_approximate))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.settings_permission_granted))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.settings_permission_notice_title))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(string(R.string.settings_go_to_settings))
            .assertCountEquals(1)
    }

    @Test
    fun clickGoToSettingsButton_opensTheSystemSettings() {
        setContent(
            SettingsUiState(
                locationStatus = LocationStatus.PERMISSION_DENIED,
                cameraPermissionStatus = CameraPermissionStatus.DENIED,
            ),
        )

        composeTestRule
            .onNodeWithText(string(R.string.settings_go_to_settings))
            .performScrollTo()
            .performClick()

        assertEquals(1, systemSettingsClicks)
    }

    @Test
    fun missingPermissionRows_areClickable_andOpenTheSystemSettings() {
        setContent(
            SettingsUiState(
                locationStatus = LocationStatus.APPROXIMATE_ONLY,
                cameraPermissionStatus = CameraPermissionStatus.DENIED,
            ),
        )

        composeTestRule.onNodeWithText(string(R.string.settings_location_label)).performClick()
        composeTestRule.onNodeWithText(string(R.string.settings_camera_label)).performClick()

        assertEquals(2, systemSettingsClicks)
    }

    @Test
    fun grantedPermissionRows_areClickable_andOpenTheSystemSettings() {
        setContent(
            SettingsUiState(
                locationStatus = LocationStatus.READY,
                cameraPermissionStatus = CameraPermissionStatus.GRANTED,
            ),
        )

        composeTestRule.onNodeWithText(string(R.string.settings_location_label)).performClick()
        composeTestRule.onNodeWithText(string(R.string.settings_camera_label)).performClick()

        assertEquals(2, systemSettingsClicks)
    }

    @Test
    fun sections_showOnlyPermissions_andNoAdvancedSection() {
        setContent(SettingsUiState())

        composeTestRule
            .onNodeWithText(string(R.string.settings_permissions_section))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.settings_advanced_section))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(string(R.string.settings_demo_mode_label))
            .assertDoesNotExist()
    }

    @Test
    fun backButton_triggersOnBackClick() {
        setContent(SettingsUiState())

        composeTestRule.onNodeWithContentDescription(string(R.string.settings_back)).performClick()
        assertEquals(1, backClicks)
    }
}
