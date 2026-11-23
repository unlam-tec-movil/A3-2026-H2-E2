package com.trazo.app.ui.screens.saverun

import androidx.lifecycle.SavedStateHandle
import com.trazo.app.data.repository.FakeRunSessionRepository
import com.trazo.app.data.repository.FakeSavedRunRepository
import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunMetrics
import com.trazo.app.domain.usecases.SaveRunUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SaveRunViewModelTest {
    private val runSessions = FakeRunSessionRepository()
    private val savedRuns = FakeSavedRunRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        runSessions.putFinishedSummary(summary())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `escribir el titulo lo guarda en el estado`() {
        val viewModel = viewModel()

        viewModel.onTitleChange("Mi carrera")

        assertEquals("Mi carrera", viewModel.uiState.value.title)
    }

    @Test
    fun `marcar un destacado lo agrega y volver a marcarlo lo quita`() {
        val viewModel = viewModel()

        viewModel.onHighlightToggle("Sin parar")
        assertEquals(setOf("Sin parar"), viewModel.uiState.value.highlights)

        viewModel.onHighlightToggle("Sin parar")
        assertTrue(
            viewModel.uiState.value.highlights
                .isEmpty(),
        )
    }

    @Test
    fun `guardar con un titulo valido avisa que se guardo`() =
        runTest {
            val viewModel = viewModel()
            viewModel.onTitleChange("Mi carrera")

            viewModel.onSave()

            assertEquals(SaveRunEffect.Saved, viewModel.effect.first())
            assertEquals("Mi carrera", savedRuns.getById("r1")?.title)
        }

    @Test
    fun `un titulo invalido marca el error y escribir de nuevo lo borra`() {
        val viewModel = viewModel()
        viewModel.onTitleChange("ab")

        viewModel.onSave()
        assertTrue(viewModel.uiState.value.titleError)

        viewModel.onTitleChange("abc")
        assertFalse(viewModel.uiState.value.titleError)
    }

    @Test
    fun `si el recorrido ya no existe se cierra la pantalla`() =
        runTest {
            val viewModel = viewModel(runId = "otro")
            viewModel.onTitleChange("Mi carrera")

            viewModel.onSave()

            assertEquals(SaveRunEffect.Close, viewModel.effect.first())
        }

    private fun viewModel(runId: String = "r1") =
        SaveRunViewModel(
            savedStateHandle = SavedStateHandle(mapOf("runId" to runId)),
            saveRun = SaveRunUseCase(runSessions, savedRuns),
        )

    private fun summary() =
        FrozenRunSummary(
            id = "r1",
            goal = RunGoal.FIVE_KILOMETERS,
            routePoints = emptyList(),
            metrics = RunMetrics(elapsedTimeSeconds = 1800, distanceMeters = 5000.0, averageSpeedKmh = 10.0),
            startedAt = Instant.fromEpochMilliseconds(0L),
            startedAtMonotonicTimestamp = 0L,
            finishedAtMonotonicTimestamp = 2000L,
            goalReached = true,
        )
}
