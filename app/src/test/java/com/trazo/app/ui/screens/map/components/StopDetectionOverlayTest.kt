package com.trazo.app.ui.screens.map.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.ui.screens.map.BrakeAlertUi
import com.trazo.app.ui.screens.map.StopDetectionUiState
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StopDetectionOverlayTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private val fullState =
        StopDetectionUiState(alert = BrakeAlertUi(brakeId = "b1"), classificationVisible = true)

    @Test
    fun suppressed_showsNeitherAlertNorDialog() {
        composeTestRule.setContent {
            TrazoTheme {
                StopDetectionOverlayContent(uiState = fullState, suppressed = true, onClassify = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.map_brake_alert_title)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.map_stop_query_title)).assertDoesNotExist()
    }

    @Test
    fun notSuppressed_showsAlertAndDialogAccordingToState() {
        composeTestRule.setContent {
            TrazoTheme {
                StopDetectionOverlayContent(uiState = fullState, suppressed = false, onClassify = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.map_brake_alert_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.map_stop_query_title)).assertIsDisplayed()
    }

    @Test
    fun notSuppressed_emptyState_showsNothing() {
        composeTestRule.setContent {
            TrazoTheme {
                StopDetectionOverlayContent(
                    uiState = StopDetectionUiState(),
                    suppressed = false,
                    onClassify = {},
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.map_brake_alert_title)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.map_stop_query_title)).assertDoesNotExist()
    }
}
