package com.trazo.app.ui.components.layout

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.R
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.ui.screens.map.LiveRouteTrackingUiState
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RunPanelTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun showPanel(
        state: LiveRouteTrackingUiState,
        controls: String? = null,
    ) {
        composeTestRule.setContent {
            TrazoTheme {
                RunPanel(uiState = state) { controls?.let { Text(it) } }
            }
        }
    }

    @Test
    fun `given a run in progress, when the panel is shown, then displays time and metrics`() {
        // Given
        val state =
            LiveRouteTrackingUiState(
                hasActiveRun = true,
                runState = RunState.ACTIVE,
                formattedTime = "00:06:42",
                formattedDistance = "1.25",
                formattedSpeed = "9.5",
                formattedAverageSpeed = "8.7",
            )

        // When
        showPanel(state)

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.map_elapsed_time)).assertIsDisplayed()
        composeTestRule.onNodeWithText("00:06:42").assertIsDisplayed()
        composeTestRule.onNodeWithText("1.25").assertIsDisplayed()
        composeTestRule.onNodeWithText("9.5").assertIsDisplayed()
        composeTestRule.onNodeWithText("8.7").assertIsDisplayed()
    }

    @Test
    fun `given a goal, when the panel is shown, then displays the goal badge`() {
        // Given
        val state = LiveRouteTrackingUiState(goal = RunGoal.FIVE_KILOMETERS)

        // When
        showPanel(state)

        // Then
        val badge = context.getString(R.string.map_goal_badge, context.getString(R.string.map_goal_5km))
        composeTestRule.onNodeWithText(badge).assertIsDisplayed()
    }

    @Test
    fun `given no goal, when the panel is shown, then no goal badge is displayed`() {
        // Given
        val state = LiveRouteTrackingUiState(goal = null)

        // When
        showPanel(state)

        // Then
        val badge = context.getString(R.string.map_goal_badge, context.getString(R.string.map_goal_generic))
        composeTestRule.onNodeWithText(badge).assertDoesNotExist()
    }

    @Test
    fun `given a controls slot, when the panel is shown, then renders the slot content`() {
        // Given
        val state = LiveRouteTrackingUiState()

        // When
        showPanel(state, controls = "slot-content")

        // Then
        composeTestRule.onNodeWithText("slot-content").assertIsDisplayed()
    }
}
