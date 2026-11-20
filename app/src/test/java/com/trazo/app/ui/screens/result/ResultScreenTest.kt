package com.trazo.app.ui.screens.result

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.R
import com.trazo.app.data.repository.FakeRunSessionRepository
import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunMetrics
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Clock

@RunWith(AndroidJUnit4::class)
class ResultScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val repository = FakeRunSessionRepository()
    private val continued = mutableListOf<String>()
    private var leaves = 0

    private fun createViewModel(runId: String?) =
        ResultViewModel(
            savedStateHandle = SavedStateHandle(if (runId == null) emptyMap() else mapOf("runId" to runId)),
            runSessionRepository = repository,
            formatter = ResultFormatter(),
        )

    private fun showScreen(viewModel: ResultViewModel) {
        composeTestRule.setContent {
            TrazoTheme {
                ResultScreen(
                    onContinue = { continued.add(it) },
                    onLeave = { leaves++ },
                    viewModel = viewModel,
                )
            }
        }
    }

    private fun summary() =
        FrozenRunSummary(
            id = "run-1",
            goal = RunGoal.FIVE_KILOMETERS,
            routePoints = emptyList(),
            metrics = RunMetrics(),
            startedAt = Clock.System.now(),
            startedAtMonotonicTimestamp = 0L,
            finishedAtMonotonicTimestamp = 1_000L,
            goalReached = true,
        )

    @Test
    fun `given a finished run, when the result screen is shown, then it shows the continue action and does not leave`() {
        // Given
        repository.putFinishedSummary(summary())
        showScreen(createViewModel("run-1"))
        val continueLabel = composeTestRule.activity.getString(R.string.result_continue)

        // When
        composeTestRule.waitForIdle()

        // Then
        composeTestRule.onNodeWithText(continueLabel).assertExists()
        assertEquals(0, leaves)
        assertEquals(0, continued.size)
    }

    @Test
    fun `given an unknown run, when the result screen is shown, then it leaves to the map`() {
        // Given / When
        showScreen(createViewModel("missing"))
        composeTestRule.waitForIdle()

        // Then
        assertEquals(1, leaves)
    }
}
