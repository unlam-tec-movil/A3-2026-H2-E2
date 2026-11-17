package com.trazo.app.ui.screens.result

import androidx.lifecycle.SavedStateHandle
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunMetrics
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.repository.FakeRunSessionRepository
import com.trazo.app.domain.usecases.FinishRunUseCase
import com.trazo.app.domain.usecases.GetFinishedRunSummaryUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Clock

/** Flujo cierre -> resultado con el fake del repositorio: finaliza una sesión y abre el resultado con su id. */
class ResultFlowTest {
    private val repository = FakeRunSessionRepository()

    private suspend fun finishAndOpenResult(
        distanceMeters: Double,
        goalReached: Boolean,
        goal: RunGoal,
    ): ResultUiState {
        repository.setSession(
            RunSession.ActiveSession(
                id = "run-flow",
                goal = goal,
                state = RunState.ACTIVE,
                metrics = RunMetrics(elapsedTimeSeconds = 1930L, distanceMeters = distanceMeters),
                startMonotonicTimestamp = 1000L,
                startedAt = Clock.System.now(),
                goalReached = goalReached,
            ),
        )
        val summary = FinishRunUseCase(repository).invoke().getOrThrow()
        return ResultViewModel(
            savedStateHandle = SavedStateHandle(mapOf("runId" to summary.id)),
            getFinishedRunSummary = GetFinishedRunSummaryUseCase(repository),
            formatter = ResultFormatter(),
        ).uiState.value
    }

    @Test
    fun `finishing with the goal reached opens the goal reached result`() =
        runTest {
            val state = finishAndOpenResult(distanceMeters = 5200.0, goalReached = true, goal = RunGoal.FIVE_KILOMETERS)

            assertTrue(state.isLoaded)
            assertTrue(state.isGoalReached)
            assertEquals("5,2 km", state.distanceText)
        }

    @Test
    fun `finishing without the goal opens the finished result`() =
        runTest {
            val state = finishAndOpenResult(distanceMeters = 1200.0, goalReached = false, goal = RunGoal.FIVE_KILOMETERS)

            assertTrue(state.isLoaded)
            assertFalse(state.isGoalReached)
            assertEquals("1,2 km", state.distanceText)
        }
}
