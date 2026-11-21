package com.trazo.app.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
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
    private var locationSettingsClicks = 0
    private var cameraSettingsClicks = 0
    private var lastToggledDemoMode: Boolean? = null

    private fun string(
        @StringRes id: Int,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setContent(state: SettingsUiState) {
        composeTestRule.setContent {
            TrazoTheme {
                SettingsContent(
                    state = state,
                    onBackClick = { backClicks++ },
                    onLocationSettingsClick = { locationSettingsClicks++ },
                    onCameraSettingsClick = { cameraSettingsClicks++ },
                    onDemoModeToggle = { lastToggledDemoMode = it },
                )
            }
        }
    }

    @Test
    fun readyLocationAndGrantedCamera_showsExactAndGranted_andNoSettingsButton() {
        setContent(
            SettingsUiState(
                locationStatus = LocationStatus.READY,
                cameraPermissionStatus = CameraPermissionStatus.GRANTED,
            ),
        )

        composeTestRule.onNodeWithText(string(R.string.settings_permission_exact)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.settings_permission_granted)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.settings_go_to_settings)).assertDoesNotExist()
    }

    @Test
    fun deviceLocationOffAndGrantedCamera_showsExactAndGranted_andNoSettingsButton() {
        setContent(
            SettingsUiState(
                locationStatus = LocationStatus.DEVICE_LOCATION_OFF,
                cameraPermissionStatus = CameraPermissionStatus.GRANTED,
            ),
        )

        composeTestRule.onNodeWithText(string(R.string.settings_permission_exact)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.settings_permission_granted)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.settings_go_to_settings)).assertDoesNotExist()
    }

    @Test
    fun deniedLocationAndDeniedCamera_showsDeniedAndTwoSettingsButtons() {
        setContent(
            SettingsUiState(
                locationStatus = LocationStatus.PERMISSION_DENIED,
                cameraPermissionStatus = CameraPermissionStatus.DENIED,
            ),
        )

        composeTestRule.onAllNodesWithText(string(R.string.settings_permission_denied)).assertCountEquals(2)
        composeTestRule.onAllNodesWithText(string(R.string.settings_go_to_settings)).assertCountEquals(2)
    }

    @Test
    fun deniedLocationAndUnknownCamera_showsDeniedAndTwoSettingsButtons() {
        setContent(
            SettingsUiState(
                locationStatus = LocationStatus.PERMISSION_DENIED,
                cameraPermissionStatus = CameraPermissionStatus.UNKNOWN,
            ),
        )

        composeTestRule.onAllNodesWithText(string(R.string.settings_permission_denied)).assertCountEquals(2)
        composeTestRule.onAllNodesWithText(string(R.string.settings_go_to_settings)).assertCountEquals(2)
    }

    @Test
    fun approximateLocationAndGrantedCamera_showsApproximateAndGranted_andOneSettingsButton() {
        setContent(
            SettingsUiState(
                locationStatus = LocationStatus.APPROXIMATE_ONLY,
                cameraPermissionStatus = CameraPermissionStatus.GRANTED,
            ),
        )

        composeTestRule.onNodeWithText(string(R.string.settings_permission_approximate)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.settings_permission_granted)).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(string(R.string.settings_go_to_settings)).assertCountEquals(1)
    }

    @Test
    fun clickLocationAndCameraSettingsButtons_triggersRespectiveCallbacks() {
        setContent(
            SettingsUiState(
                locationStatus = LocationStatus.PERMISSION_DENIED,
                cameraPermissionStatus = CameraPermissionStatus.DENIED,
            ),
        )

        val buttons = composeTestRule.onAllNodesWithText(string(R.string.settings_go_to_settings))
        buttons[0].performClick()
        assertEquals(1, locationSettingsClicks)
        assertEquals(0, cameraSettingsClicks)

        buttons[1].performClick()
        assertEquals(1, locationSettingsClicks)
        assertEquals(1, cameraSettingsClicks)
    }

    @Test
    fun demoModeWithRunNotInProgressAndDemoEnabled_switchEnabled_showsActiveNotice_andToggleSendsFalse() {
        setContent(
            SettingsUiState(
                isDemoModeEnabled = true,
                isRunInProgress = false,
            ),
        )

        composeTestRule
            .onNode(isToggleable())
            .performScrollTo()
            .assertIsEnabled()
            .assertIsOn()
            .performClick()
        composeTestRule.onNodeWithText(string(R.string.settings_demo_mode_active_notice)).performScrollTo().assertIsDisplayed()
        assertEquals(false, lastToggledDemoMode)
    }

    @Test
    fun demoModeWithRunNotInProgressAndDemoDisabled_toggleSendsTrue() {
        setContent(
            SettingsUiState(
                isDemoModeEnabled = false,
                isRunInProgress = false,
            ),
        )

        composeTestRule
            .onNode(isToggleable())
            .performScrollTo()
            .assertIsEnabled()
            .assertIsOff()
            .performClick()
        assertEquals(true, lastToggledDemoMode)
    }

    @Test
    fun demoModeWithRunInProgress_switchDisabled_andShowsRunningNotice() {
        setContent(
            SettingsUiState(
                isDemoModeEnabled = false,
                isRunInProgress = true,
            ),
        )

        composeTestRule.onNode(isToggleable()).performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithText(string(R.string.settings_demo_mode_running_notice)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun backButton_triggersOnBackClick() {
        setContent(SettingsUiState())

        composeTestRule.onNodeWithContentDescription(string(R.string.settings_back)).performClick()
        assertEquals(1, backClicks)
    }
}
