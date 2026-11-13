package com.trazo.app.ui.screens.map

import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RawLocationPoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunMetrics
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.repository.LocationClient
import com.trazo.app.domain.repository.RunSessionRepository
import com.trazo.app.domain.usecases.AttachActiveRunSessionUseCase
import com.trazo.app.domain.usecases.FinishRunUseCase
import com.trazo.app.domain.usecases.GetActiveRunSessionUseCase
import com.trazo.app.domain.usecases.PauseRunUseCase
import com.trazo.app.domain.usecases.ResumeRunUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
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
    private lateinit var fakeLocationClient: FakeLocationClient
    private lateinit var viewModel: LiveRouteTrackingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeRepository = FakeRunSessionRepository()
        fakeLocationClient = FakeLocationClient()

        val getSessionUseCase = GetActiveRunSessionUseCase(fakeRepository)
        val attachUseCase = AttachActiveRunSessionUseCase(fakeRepository)
        val pauseUseCase = PauseRunUseCase(fakeRepository)
        val resumeUseCase = ResumeRunUseCase(fakeRepository)
        val finishUseCase = FinishRunUseCase(fakeRepository)

        viewModel =
            LiveRouteTrackingViewModel(
                getActiveRunSessionUseCase = getSessionUseCase,
                attachActiveRunSessionUseCase = attachUseCase,
                pauseRunUseCase = pauseUseCase,
                resumeRunUseCase = resumeUseCase,
                finishRunUseCase = finishUseCase,
                runSessionRepository = fakeRepository,
                locationClient = fakeLocationClient,
            )
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

    private class FakeRunSessionRepository : RunSessionRepository {
        private val sessionStateFlow = MutableStateFlow<RunSession>(RunSession.Idle)
        override val activeSession: StateFlow<RunSession> = sessionStateFlow

        private val inProgressStateFlow = MutableStateFlow(false)
        override val isRunInProgress: StateFlow<Boolean> = inProgressStateFlow

        private val summaries = HashMap<String, FrozenRunSummary>()

        fun setSession(session: RunSession) {
            sessionStateFlow.value = session
            inProgressStateFlow.value = session is RunSession.ActiveSession
        }

        override fun attachActiveSession(session: InitializedRunSession) {
            setSession(
                RunSession.ActiveSession(
                    id = session.id,
                    goal = session.goal,
                    state = RunState.ACTIVE,
                    startMonotonicTimestamp = session.startMonotonicTimestamp,
                    startedAt = session.startedAt,
                    isDemoMode = session.isDemoMode,
                ),
            )
        }

        override suspend fun pauseSession(): Result<Unit> {
            val current =
                sessionStateFlow.value as? RunSession.ActiveSession
                    ?: return Result.failure(IllegalStateException("No active run"))
            sessionStateFlow.value = current.copy(state = RunState.PAUSED)
            return Result.success(Unit)
        }

        override suspend fun pausePersonally(): Result<Unit> {
            val current =
                sessionStateFlow.value as? RunSession.ActiveSession
                    ?: return Result.failure(IllegalStateException("No active run"))
            sessionStateFlow.value = current.copy(state = RunState.PERSONAL_PAUSED)
            return Result.success(Unit)
        }

        override suspend fun markStopped(): Result<Unit> {
            val current =
                sessionStateFlow.value as? RunSession.ActiveSession
                    ?: return Result.failure(IllegalStateException("No active run"))
            sessionStateFlow.value = current.copy(state = RunState.STOPPED)
            return Result.success(Unit)
        }

        override suspend fun markMoving(): Result<Unit> {
            val current =
                sessionStateFlow.value as? RunSession.ActiveSession
                    ?: return Result.failure(IllegalStateException("No active run"))
            if (current.state != RunState.STOPPED) {
                return Result.failure(IllegalStateException("Not stopped"))
            }
            sessionStateFlow.value = current.copy(state = RunState.ACTIVE)
            return Result.success(Unit)
        }

        override suspend fun resumeSession(): Result<Unit> {
            val current =
                sessionStateFlow.value as? RunSession.ActiveSession
                    ?: return Result.failure(IllegalStateException("No active run"))
            if (current.state != RunState.PAUSED && current.state != RunState.PERSONAL_PAUSED) {
                return Result.failure(IllegalStateException("Cannot manually resume"))
            }
            sessionStateFlow.value = current.copy(state = RunState.ACTIVE)
            return Result.success(Unit)
        }

        override suspend fun finishSession(): Result<FrozenRunSummary> {
            val current =
                sessionStateFlow.value as? RunSession.ActiveSession
                    ?: return Result.failure(IllegalStateException("No active run"))

            val summary =
                FrozenRunSummary(
                    id = current.id,
                    goal = current.goal,
                    routePoints = current.routePoints,
                    metrics = current.metrics,
                    startedAt = current.startedAt,
                    startedAtMonotonicTimestamp = current.startMonotonicTimestamp,
                    finishedAtMonotonicTimestamp = 2000L,
                    goalReached = current.goalReached,
                    isDemoMode = current.isDemoMode,
                )

            summaries[summary.id] = summary
            resetToIdle()

            return Result.success(summary)
        }

        override fun resetToIdle() {
            sessionStateFlow.value = RunSession.Idle
            inProgressStateFlow.value = false
        }

        override fun getFinishedSummary(id: String): FrozenRunSummary? = summaries[id]
    }

    private class FakeLocationClient : LocationClient {
        override fun getLocationUpdates(intervalMillis: Long): Flow<RawLocationPoint> = emptyFlow()

        override fun getSignalQualityUpdates(): Flow<GpsQuality> = emptyFlow()
    }
}
