package com.trazo.app.ui.screens.result

import androidx.lifecycle.SavedStateHandle
import com.trazo.app.data.repository.FakeRunSessionRepository
import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunMetrics
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Clock

class ResultViewModelTest {
    private val repository = FakeRunSessionRepository()

    private fun viewModel(runId: String) =
        ResultViewModel(
            savedStateHandle = SavedStateHandle(mapOf("runId" to runId)),
            runSessionRepository = repository,
        )

    @Test
    fun `with goal reached the state shows title data from the summary`() {
        repository.putFinishedSummary(summary("run-1", goalReached = true))

        val state = viewModel("run-1").uiState.value

        assertTrue(state.isLoaded)
        assertTrue(state.isGoalReached)
        assertEquals(RunGoal.FIVE_KILOMETERS, state.goal)
        assertEquals("5,2 km", state.distanceText)
        assertEquals("00:32:10", state.timeText)
    }

    @Test
    fun `without goal reached the state is not marked as goal reached`() {
        repository.putFinishedSummary(summary("run-1", goalReached = false))

        val state = viewModel("run-1").uiState.value

        assertTrue(state.isLoaded)
        assertFalse(state.isGoalReached)
        assertEquals("5,2 km", state.distanceText)
    }

    @Test
    fun `regularity is absent when the summary has none`() {
        repository.putFinishedSummary(summary("run-1", goalReached = true))

        assertNull(viewModel("run-1").uiState.value.regularityText)
    }

    @Test
    fun `regularity is formatted from the summary`() {
        repository.putFinishedSummary(summary("run-1", goalReached = true, regularityPercent = 80))

        assertEquals("80 %", viewModel("run-1").uiState.value.regularityText)
    }

    @Test
    fun `metrics are formatted from the summary without recalculating`() {
        repository.putFinishedSummary(
            summary("run-1", goalReached = false, metrics = RunMetrics(elapsedTimeSeconds = 80L, distanceMeters = 1234.0)),
        )

        val state = viewModel("run-1").uiState.value

        assertEquals("1,2 km", state.distanceText)
        assertEquals("00:01:20", state.timeText)
    }

    @Test
    fun `unknown id emits LeaveToMap and stays not loaded`() =
        kotlinx.coroutines.test.runTest {
            val vm = viewModel("unknown")

            assertFalse(vm.uiState.value.isLoaded)
            assertEquals(ResultEffect.LeaveToMap, vm.effect.first())
        }

    @Test
    fun `reads the summary for the id from the saved state`() {
        repository.putFinishedSummary(summary("run-9", goalReached = true))

        viewModel("run-9")

        assertEquals(listOf("run-9"), repository.getFinishedSummaryCalls)
    }

    private fun summary(
        id: String,
        goalReached: Boolean,
        metrics: RunMetrics = RunMetrics(elapsedTimeSeconds = 1930L, distanceMeters = 5200.0),
        regularityPercent: Int? = null,
    ) = FrozenRunSummary(
        id = id,
        goal = RunGoal.FIVE_KILOMETERS,
        routePoints = emptyList(),
        metrics = metrics,
        startedAt = Clock.System.now(),
        startedAtMonotonicTimestamp = 1000L,
        finishedAtMonotonicTimestamp = 2000L,
        goalReached = goalReached,
        regularityPercent = regularityPercent,
    )
}
