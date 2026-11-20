package com.trazo.app.ui.screens.map

import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.repository.FakeRunSessionRepository
import com.trazo.app.domain.repository.LocationClient
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class LiveRouteTrackingViewModelFlowsTest {
    private val repository = FakeRunSessionRepository()
    private var locationPoints: List<RoutePoint> = emptyList()
    private var signalFailure: Throwable? = null

    private val locationClient =
        object : LocationClient {
            override fun getLocationUpdates(intervalMillis: Long): Flow<RoutePoint> = flowOf(*locationPoints.toTypedArray())

            override fun getSignalQualityUpdates(): Flow<GpsQuality> = flow { signalFailure?.let { throw it } }
        }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        LiveRouteTrackingViewModel(
            runSessionRepository = repository,
            locationClient = locationClient,
        )

    private fun session(state: RunState) =
        RunSession.ActiveSession(
            id = "run-1",
            goal = RunGoal.THREE_KILOMETERS,
            state = state,
            startMonotonicTimestamp = 0L,
            startedAt = Clock.System.now(),
        )

    private fun rawPoint() =
        RoutePoint(
            latitude = -34.6,
            longitude = -58.4,
            accuracyMeters = 5f,
            speedMetersPerSecond = 2f,
            monotonicTimestampMillis = 10L,
        )

    private fun kotlinx.coroutines.test.TestScope.keepStateHot(viewModel: LiveRouteTrackingViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun `given no active run, when the device reports a location, then the runner position is exposed`() =
        runTest {
            // Given
            locationPoints = listOf(rawPoint())
            val viewModel = createViewModel()

            // When
            keepStateHot(viewModel)

            // Then
            assertEquals(
                -34.6,
                viewModel.uiState.value.runnerPosition
                    ?.latitude ?: 0.0,
                0.0001,
            )
        }

    @Test
    fun `given a paused run, when the device reports a location, then the runner position is exposed`() =
        runTest {
            // Given
            locationPoints = listOf(rawPoint())
            repository.setSession(session(RunState.PAUSED))
            val viewModel = createViewModel()

            // When
            keepStateHot(viewModel)

            // Then
            assertNotNull(viewModel.uiState.value.runnerPosition)
        }

    @Test
    fun `given an active run, when the runner pauses and resumes, then the session state follows`() =
        runTest {
            // Given
            repository.setSession(session(RunState.ACTIVE))
            val viewModel = createViewModel()

            // When
            viewModel.onPauseClicked()
            val pausedState = (repository.activeSession.value as RunSession.ActiveSession).state
            viewModel.onResumeClicked()
            val resumedState = (repository.activeSession.value as RunSession.ActiveSession).state

            // Then
            assertEquals(RunState.PAUSED, pausedState)
            assertEquals(RunState.ACTIVE, resumedState)
        }

    @Test
    fun `given an active run, when confirming the finish, then navigates to the results`() =
        runTest {
            // Given
            repository.setSession(session(RunState.ACTIVE))
            val viewModel = createViewModel()

            // When
            viewModel.onConfirmFinish()
            val effect = viewModel.uiEffect.first()

            // Then
            assertTrue(effect is LiveRouteTrackingUiEffect.NavigateToResults)
        }

    @Test
    fun `given an active run, when the permission is revoked, then warns about the lost permission`() =
        runTest {
            // Given
            repository.setSession(session(RunState.ACTIVE))
            val viewModel = createViewModel()
            val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEffect.first() }

            // When
            viewModel.onLocationPermissionRevoked()

            // Then
            assertTrue(effect.await() is LiveRouteTrackingUiEffect.ShowLocationPermissionLostMessage)
        }

    @Test
    fun `given an active run, when the signal updates fail with a security exception at creation, then warns about the lost permission`() =
        runTest {
            // Given
            signalFailure = SecurityException("revoked")
            repository.setSession(session(RunState.ACTIVE))

            // When
            val viewModel = createViewModel()
            val effect = viewModel.uiEffect.first()

            // Then
            assertTrue(effect is LiveRouteTrackingUiEffect.ShowLocationPermissionLostMessage)
        }

    @Test
    fun `given no active run, when the permission is revoked, then no warning is emitted`() =
        runTest {
            // Given
            val viewModel = createViewModel()

            // When
            viewModel.onLocationPermissionRevoked()
            viewModel.onStartRunClicked()
            val effect = viewModel.uiEffect.first()

            // Then
            assertEquals(LiveRouteTrackingUiEffect.OpenStartRunModal, effect)
        }
}
