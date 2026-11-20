package com.trazo.app.ui.components.layout

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.ui.screens.map.LiveRouteTrackingUiState
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RunControlsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var pauseClicks = 0
    private var resumeClicks = 0
    private var finishClicks = 0

    private fun string(
        @StringRes id: Int,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setContent(state: LiveRouteTrackingUiState) {
        composeTestRule.setContent {
            TrazoTheme {
                RunControls(
                    uiState = state,
                    onPause = { pauseClicks++ },
                    onResume = { resumeClicks++ },
                    onFinish = { finishClicks++ },
                )
            }
        }
    }

    private fun runState(state: RunState) =
        LiveRouteTrackingUiState(
            hasActiveRun = true,
            runState = state,
            areControlActionsAllowed = true,
        )

    @Test
    fun activeRun_tapPause_callsOnPause() {
        setContent(runState(RunState.ACTIVE))

        composeTestRule.onNodeWithContentDescription(string(R.string.map_pause)).performClick()

        assertEquals(1, pauseClicks)
        assertEquals(0, resumeClicks)
    }

    // En una frenada el recorrido sigue en curso: se ve Pausar, habilitado, y también Finalizar.
    @Test
    fun stoppedRun_showsEnabledPause_andCallsOnPause() {
        setContent(runState(RunState.STOPPED))

        composeTestRule.onNodeWithContentDescription(string(R.string.map_pause)).assertIsEnabled().performClick()
        composeTestRule.onNodeWithContentDescription(string(R.string.map_finish)).assertIsEnabled()

        assertEquals(1, pauseClicks)
        assertEquals(0, resumeClicks)
    }

    @Test
    fun pausedRun_showsResume_andCallsOnResume() {
        setContent(runState(RunState.PAUSED))

        composeTestRule.onNodeWithContentDescription(string(R.string.map_resume)).performClick()

        assertEquals(1, resumeClicks)
        assertEquals(0, pauseClicks)
    }

    @Test
    fun activeRun_tapFinish_callsOnFinish() {
        setContent(runState(RunState.ACTIVE))

        composeTestRule.onNodeWithContentDescription(string(R.string.map_finish)).performClick()

        assertEquals(1, finishClicks)
    }

    @Test
    fun noRun_controlsAreDisabled() {
        setContent(LiveRouteTrackingUiState())

        composeTestRule.onNodeWithContentDescription(string(R.string.map_pause)).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(string(R.string.map_finish)).assertIsNotEnabled()
    }

    @Test
    fun controls_measure56dp() {
        setContent(runState(RunState.ACTIVE))

        listOf(R.string.map_pause, R.string.map_finish).forEach { id ->
            composeTestRule
                .onNodeWithContentDescription(string(id))
                .assertIsEnabled()
                .assertWidthIsEqualTo(Sizes.runControl)
                .assertHeightIsEqualTo(Sizes.runControl)
        }
    }
}
