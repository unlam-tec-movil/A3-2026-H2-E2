package com.trazo.app.ui.screens.map

import com.trazo.app.data.repository.FakeLocationRepository
import com.trazo.app.data.repository.FakeRunSessionRepository
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunMetrics
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class LiveRouteTrackingViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeRunSessionRepository
    private lateinit var fakeLocationRepository: FakeLocationRepository
    private lateinit var viewModel: LiveRouteTrackingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeRepository = FakeRunSessionRepository()
        fakeLocationRepository = FakeLocationRepository()

        viewModel =
            testLiveRouteTrackingViewModel(fakeRepository, fakeLocationRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onFinishRequested_withoutActiveRun_doesNothing() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            testScheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.hasActiveRun)

            viewModel.onFinishRequested()
            testScheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showFinishConfirmDialog)
            assertFalse(viewModel.uiState.value.showShortRunDialog)
        }

    @Test
    fun onFinishRequested_withLessThan200Meters_showsShortRunDialog() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }

            fakeRepository.setSession(
                RunSession.ActiveSession(
                    id = "session-1",
                    goal = RunGoal.THREE_KILOMETERS,
                    state = RunState.ACTIVE,
                    metrics = RunMetrics(distanceMeters = 150.0),
                    startMonotonicTimestamp = 1000L,
                    startedAt = Clock.System.now(),
                ),
            )
            testScheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.hasActiveRun)

            viewModel.onFinishRequested()
            testScheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showShortRunDialog)
            assertFalse(viewModel.uiState.value.showFinishConfirmDialog)
            assertTrue(viewModel.uiState.value.suppressed)
        }

    @Test
    fun onFinishRequested_withAtLeast200Meters_showsFinishConfirmDialog() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }

            fakeRepository.setSession(
                RunSession.ActiveSession(
                    id = "session-1",
                    goal = RunGoal.THREE_KILOMETERS,
                    state = RunState.ACTIVE,
                    metrics = RunMetrics(distanceMeters = 250.0),
                    startMonotonicTimestamp = 1000L,
                    startedAt = Clock.System.now(),
                ),
            )
            testScheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.hasActiveRun)

            viewModel.onFinishRequested()
            testScheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showFinishConfirmDialog)
            assertFalse(viewModel.uiState.value.showShortRunDialog)
            assertTrue(viewModel.uiState.value.suppressed)
        }

    @Test
    fun uiState_reflectsGoalReachedStateCorrectly() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }

            fakeRepository.setSession(
                RunSession.ActiveSession(
                    id = "session-1",
                    goal = RunGoal.THREE_KILOMETERS,
                    state = RunState.ACTIVE,
                    metrics = RunMetrics(distanceMeters = 3050.0),
                    goalReached = true,
                    startMonotonicTimestamp = 1000L,
                    startedAt = Clock.System.now(),
                ),
            )
            testScheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.goalReached)
            assertTrue(viewModel.uiState.value.showGoalCelebration)
            assertTrue(viewModel.uiState.value.suppressed)
        }

    @Test
    fun onDiscardShortRun_resetsSessionToIdle() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }

            fakeRepository.setSession(
                RunSession.ActiveSession(
                    id = "session-1",
                    goal = RunGoal.THREE_KILOMETERS,
                    state = RunState.ACTIVE,
                    metrics = RunMetrics(distanceMeters = 50.0),
                    startMonotonicTimestamp = 1000L,
                    startedAt = Clock.System.now(),
                ),
            )
            testScheduler.advanceUntilIdle()

            viewModel.onFinishRequested()
            testScheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showShortRunDialog)

            viewModel.onDiscardShortRun()
            testScheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showShortRunDialog)
            assertEquals(RunSession.Idle, fakeRepository.activeSession.value)
            assertFalse(fakeRepository.isRunInProgress.value)
            assertFalse(viewModel.uiState.value.suppressed)
        }

    @Test
    fun repository_handlesSummaryRecoveryById() =
        runTest {
            val now = Clock.System.now()
            fakeRepository.setSession(
                RunSession.ActiveSession(
                    id = "run-123",
                    goal = RunGoal.FIVE_KILOMETERS,
                    state = RunState.ACTIVE,
                    metrics = RunMetrics(distanceMeters = 5200.0, elapsedTimeSeconds = 1500L),
                    startMonotonicTimestamp = 1000L,
                    startedAt = now,
                    isDemoMode = true,
                    goalReached = true,
                ),
            )

            val finishResult = fakeRepository.finishSession()
            assertTrue(finishResult.isSuccess)

            val summary = fakeRepository.getFinishedSummary("run-123")
            assertNotNull(summary)
            assertEquals("run-123", summary?.id)
            assertEquals(RunGoal.FIVE_KILOMETERS, summary?.goal)
            assertEquals(now, summary?.startedAt)
            assertTrue(summary?.goalReached == true)
            assertTrue(summary?.isDemoMode == true)

            assertEquals(RunSession.Idle, fakeRepository.activeSession.value)
        }

    // «Volver al recorrido» con menos de 200 m: si mientras el aviso estaba abierto el corredor quedó
    // quieto y se detectó una frenada, Pausar tiene que seguir habilitado y pausar el recorrido.
    @Test
    fun onReturnFromShortRun_duringStop_keepsPauseEnabled() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            fakeRepository.setSession(
                RunSession.ActiveSession(
                    id = "session-1",
                    goal = RunGoal.THREE_KILOMETERS,
                    state = RunState.ACTIVE,
                    metrics = RunMetrics(distanceMeters = 150.0),
                    startMonotonicTimestamp = 1000L,
                    startedAt = Clock.System.now(),
                ),
            )
            testScheduler.advanceUntilIdle()
            viewModel.onFinishRequested()
            testScheduler.advanceUntilIdle()
            assertTrue(fakeRepository.markStopped().isSuccess)

            viewModel.onReturnFromShortRun()
            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.showShortRunDialog)
            assertEquals(RunState.STOPPED, state.runState)
            assertTrue(state.canPause)
            assertTrue(state.canFinish)

            viewModel.onPauseClicked()
            testScheduler.advanceUntilIdle()

            assertEquals(RunState.PAUSED, viewModel.uiState.value.runState)
            assertTrue(viewModel.uiState.value.canResume)
        }

    private fun runWithGoal(
        id: String = "session-1",
        distanceMeters: Double,
        goalReached: Boolean,
    ) = RunSession.ActiveSession(
        id = id,
        goal = RunGoal.THREE_KILOMETERS,
        state = RunState.ACTIVE,
        metrics = RunMetrics(distanceMeters = distanceMeters),
        goalReached = goalReached,
        startMonotonicTimestamp = 1000L,
        startedAt = Clock.System.now(),
    )

    // El aviso de meta cumplida aparece una sola vez por recorrido: seguir sumando metros no lo reabre.
    @Test
    fun goalCelebration_afterDismissing_doesNotShowAgainInTheSameRun() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            fakeRepository.setSession(runWithGoal(distanceMeters = 3050.0, goalReached = true))
            testScheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showGoalCelebration)

            viewModel.onCelebrationDismissed()
            fakeRepository.setSession(runWithGoal(distanceMeters = 3400.0, goalReached = true))
            testScheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showGoalCelebration)
            assertTrue(viewModel.uiState.value.goalReached)
        }

    // Al volver a Idle (recorrido terminado o descartado) el aviso se rearma para el recorrido siguiente.
    @Test
    fun goalCelebration_afterReturningToIdle_showsAgainInTheNextRun() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            fakeRepository.setSession(runWithGoal(distanceMeters = 3050.0, goalReached = true))
            testScheduler.advanceUntilIdle()
            viewModel.onCelebrationDismissed()

            fakeRepository.resetToIdle()
            testScheduler.advanceUntilIdle()
            assertFalse(viewModel.uiState.value.showGoalCelebration)

            fakeRepository.setSession(runWithGoal(id = "session-2", distanceMeters = 100.0, goalReached = false))
            testScheduler.advanceUntilIdle()
            assertFalse(viewModel.uiState.value.showGoalCelebration)

            fakeRepository.setSession(runWithGoal(id = "session-2", distanceMeters = 3010.0, goalReached = true))
            testScheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showGoalCelebration)
        }

    // «Cancelar» en el diálogo de finalizar cierra el diálogo y deja el recorrido como estaba.
    @Test
    fun onCancelFinish_closesTheDialog_andKeepsTheRunAsItWas() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            val session = runWithGoal(distanceMeters = 1200.0, goalReached = false)
            fakeRepository.setSession(session)
            testScheduler.advanceUntilIdle()
            viewModel.onFinishRequested()
            testScheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showFinishConfirmDialog)

            viewModel.onCancelFinish()
            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.showFinishConfirmDialog)
            assertFalse(state.suppressed)
            assertEquals(RunState.ACTIVE, state.runState)
            assertTrue(state.canFinish)
            assertEquals(session, fakeRepository.activeSession.value)
        }
}
