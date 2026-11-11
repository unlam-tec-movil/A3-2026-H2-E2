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
import kotlinx.coroutines.flow.collect
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
            backgroundScope.launch { viewModel.uiState.collect() }
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.hasActiveRun)

            viewModel.onFinishRequested()
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showFinishConfirmDialog)
            assertFalse(viewModel.uiState.value.showShortRunDialog)
        }

    @Test
    fun onFinishRequested_withLessThan200Meters_showsShortRunDialog() =
        runTest {
            backgroundScope.launch { viewModel.uiState.collect() }
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

            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.hasActiveRun)

            viewModel.onFinishRequested()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showShortRunDialog)
            assertFalse(viewModel.uiState.value.showFinishConfirmDialog)
        }

    @Test
    fun onFinishRequested_withAtLeast200Meters_showsFinishConfirmDialog() =
        runTest {
            backgroundScope.launch { viewModel.uiState.collect() }
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

            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.hasActiveRun)

            viewModel.onFinishRequested()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showFinishConfirmDialog)
            assertFalse(viewModel.uiState.value.showShortRunDialog)
        }

    @Test
    fun uiState_reflectsGoalReachedStateCorrectly() =
        runTest {
            backgroundScope.launch { viewModel.uiState.collect() }
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

            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.goalReached)
        }

    @Test
    fun onDiscardShortRun_resetsSessionToIdle() =
        runTest {
            backgroundScope.launch { viewModel.uiState.collect() }
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

            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onFinishRequested()
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showShortRunDialog)

            viewModel.onDiscardShortRun()
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showShortRunDialog)
            assertEquals(RunSession.Idle, fakeRepository.activeSession.value)
            assertFalse(fakeRepository.isRunInProgress.value)
        }

    @Test
    fun onMapDragged_updatesIsMapCenteredOnRunnerToFalse() =
        runTest {
            backgroundScope.launch { viewModel.uiState.collect() }
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isMapCenteredOnRunner)

            viewModel.onMapDragged()
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isMapCenteredOnRunner)
        }

    @Test
    fun onCenterMapClicked_updatesIsMapCenteredOnRunnerToTrue() =
        runTest {
            backgroundScope.launch { viewModel.uiState.collect() }
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onMapDragged()
            testDispatcher.scheduler.advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isMapCenteredOnRunner)

            viewModel.onCenterMapClicked()
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isMapCenteredOnRunner)
        }

    @Test
    fun onLocationPermissionRevoked_duringActiveRun_emitsNoticeEffect() =
        runTest {
            backgroundScope.launch { viewModel.uiState.collect() }

            val effects = mutableListOf<LiveRouteTrackingUiEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffect.collect {
                    effects.add(it)
                }
            }
            fakeRepository.setSession(
                RunSession.ActiveSession(
                    id = "session-1",
                    goal = RunGoal.THREE_KILOMETERS,
                    state = RunState.ACTIVE,
                    startMonotonicTimestamp = 1000L,
                    startedAt = Clock.System.now(),
                ),
            )

            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.hasActiveRun)

            viewModel.onLocationPermissionRevoked()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, effects.size)
            assertTrue(effects.first() is LiveRouteTrackingUiEffect.ShowLocationPermissionLostMessage)
            val effect = effects.first() as LiveRouteTrackingUiEffect.ShowLocationPermissionLostMessage
            assertEquals(
                "Se perdió el permiso de ubicación. Activalo desde Ajustes para seguir registrando tu recorrido.",
                effect.message,
            )
        }

    @Test
    fun onLocationPermissionRevoked_calledMultipleTimes_emitsNoticeOnlyOnce() =
        runTest {
            backgroundScope.launch { viewModel.uiState.collect() }

            val effects = mutableListOf<LiveRouteTrackingUiEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffect.collect {
                    effects.add(it)
                }
            }

            fakeRepository.setSession(
                RunSession.ActiveSession(
                    id = "session-1",
                    goal = RunGoal.THREE_KILOMETERS,
                    state = RunState.ACTIVE,
                    startMonotonicTimestamp = 1000L,
                    startedAt = Clock.System.now(),
                ),
            )

            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onLocationPermissionRevoked()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onLocationPermissionRevoked()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, effects.size)
        }

    @Test
    fun onLocationPermissionRevoked_withoutActiveRun_doesNothing() =
        runTest {
            backgroundScope.launch { viewModel.uiState.collect() }

            val effects = mutableListOf<LiveRouteTrackingUiEffect>()
            backgroundScope.launch {
                viewModel.uiEffect.collect { effects.add(it) }
            }

            testDispatcher.scheduler.advanceUntilIdle()
            assertFalse(viewModel.uiState.value.hasActiveRun)

            viewModel.onLocationPermissionRevoked()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(effects.isEmpty())
        }

    private class FakeRunSessionRepository : RunSessionRepository {
        private val _activeSession = MutableStateFlow<RunSession>(RunSession.Idle)
        override val activeSession: StateFlow<RunSession> = _activeSession

        private val _isRunInProgress = MutableStateFlow(false)
        override val isRunInProgress: StateFlow<Boolean> = _isRunInProgress

        fun setSession(session: RunSession) {
            _activeSession.value = session
            _isRunInProgress.value = session is RunSession.ActiveSession
        }

        override fun attachActiveSession(session: InitializedRunSession) {
            setSession(
                RunSession.ActiveSession(
                    id = session.id,
                    goal = session.goal,
                    state = RunState.ACTIVE,
                    startMonotonicTimestamp = session.startMonotonicTimestamp,
                    startedAt = session.startedAt,
                ),
            )
        }

        override suspend fun pauseSession(): Result<Unit> = Result.success(Unit)

        override suspend fun resumeSession(): Result<Unit> = Result.success(Unit)

        override suspend fun finishSession(): Result<FrozenRunSummary> {
            val current =
                _activeSession.value as? RunSession.ActiveSession
                    ?: return Result.failure(IllegalStateException("No active run"))

            resetToIdle()

            return Result.success(
                FrozenRunSummary(
                    id = current.id,
                    goal = current.goal,
                    routePoints = current.routePoints,
                    metrics = current.metrics,
                    startedAt = current.startedAt,
                    startedAtMonotonicTimestamp = current.startMonotonicTimestamp,
                    finishedAtMonotonicTimestamp = 2000L,
                    goalReached = current.goalReached,
                ),
            )
        }

        override fun resetToIdle() {
            _activeSession.value = RunSession.Idle
            _isRunInProgress.value = false
        }
    }

    private class FakeLocationClient : LocationClient {
        override fun getLocationUpdates(intervalMillis: Long): Flow<RawLocationPoint> = emptyFlow()

        override fun getSignalQualityUpdates(): Flow<GpsQuality> = emptyFlow()
    }
}
