package com.trazo.app.data.repository

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RawLocationPoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.model.stops.BrakeEvent
import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopKind
import com.trazo.app.domain.repository.StopDetectionRepository
import com.trazo.app.domain.usecases.GpsPointFilterPolicy
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class RunSessionRepositoryImplTest {
    private lateinit var fakeLocationClient: FakeLocationClient
    private lateinit var repository: RunSessionRepositoryImpl
    private lateinit var applicationScope: CoroutineScope

    @Before
    fun setUp() {
        fakeLocationClient = FakeLocationClient()

        applicationScope =
            CoroutineScope(
                SupervisorJob() + Dispatchers.Default,
            )

        repository =
            RunSessionRepositoryImpl(
                locationClient = fakeLocationClient,
                gpsPointFilterPolicy = GpsPointFilterPolicy(),
                applicationScope = applicationScope,
            )
    }

    @After
    fun tearDown() {
        repository.resetToIdle()

        applicationScope.cancel()
    }

    @Test
    fun attachActiveSession_createsRunningSession() {
        val initializedSession =
            InitializedRunSession(
                id = "test-session",
                goal = RunGoal.THREE_KILOMETERS,
                startMonotonicTimestamp = SystemClock.elapsedRealtime(),
                startedAt = Clock.System.now(),
            )

        repository.attachActiveSession(initializedSession)

        val session = repository.activeSession.value as? RunSession.ActiveSession

        assertNotNull(session)
        assertEquals(RunState.ACTIVE, session?.state)
        assertEquals("test-session", session?.id)
        assertEquals(RunGoal.THREE_KILOMETERS, session?.goal)
        assertEquals(GpsQuality.WEAK_SIGNAL, session?.gpsQuality)
        assertTrue(repository.isRunInProgress.value)
    }

    @Test
    fun pauseSession_changesStateToPaused() =
        runBlocking {
            val initializedSession =
                InitializedRunSession(
                    id = "test-session",
                    goal = RunGoal.THREE_KILOMETERS,
                    startMonotonicTimestamp = SystemClock.elapsedRealtime(),
                    startedAt = Clock.System.now(),
                )

            repository.attachActiveSession(initializedSession)

            val result = repository.pauseSession()

            assertTrue(result.isSuccess)
            val session = repository.activeSession.value as? RunSession.ActiveSession
            assertEquals(RunState.PAUSED, session?.state)
            assertTrue(repository.isRunInProgress.value)
        }

    @Test
    fun resumeSession_changesStateToRunning() =
        runBlocking {
            val initializedSession =
                InitializedRunSession(
                    id = "test-session",
                    goal = RunGoal.THREE_KILOMETERS,
                    startMonotonicTimestamp = SystemClock.elapsedRealtime(),
                    startedAt = Clock.System.now(),
                )

            repository.attachActiveSession(initializedSession)
            repository.pauseSession()

            val result = repository.resumeSession()

            assertTrue(result.isSuccess)
            val session = repository.activeSession.value as? RunSession.ActiveSession
            assertEquals(RunState.ACTIVE, session?.state)
            assertTrue(repository.isRunInProgress.value)
        }

    @Test
    fun finishSession_returnsFrozenSummaryAndResetsToIdle() =
        runBlocking {
            val initializedSession =
                InitializedRunSession(
                    id = "test-session",
                    goal = RunGoal.THREE_KILOMETERS,
                    startMonotonicTimestamp = SystemClock.elapsedRealtime(),
                    startedAt = Clock.System.now(),
                )

            repository.attachActiveSession(initializedSession)

            val result = repository.finishSession()

            assertTrue(result.isSuccess)
            assertEquals("test-session", result.getOrNull()?.id)
            assertEquals(RunGoal.THREE_KILOMETERS, result.getOrNull()?.goal)
            assertEquals(RunSession.Idle, repository.activeSession.value)
            assertFalse(repository.isRunInProgress.value)

            val summary = repository.getFinishedSummary("test-session")
            assertNotNull(summary)
            assertEquals("test-session", summary?.id)
        }

    @Test
    fun markStopped_changesStateToStopped_andResumeSessionCannotReactivateIt() =
        runBlocking {
            val initializedSession =
                InitializedRunSession(
                    id = "test-session",
                    goal = RunGoal.THREE_KILOMETERS,
                    startMonotonicTimestamp = SystemClock.elapsedRealtime(),
                    startedAt = Clock.System.now(),
                )

            repository.attachActiveSession(initializedSession)

            val stopResult = repository.markStopped()
            assertTrue(stopResult.isSuccess)

            val stoppedSession = repository.activeSession.value as? RunSession.ActiveSession
            assertEquals(RunState.STOPPED, stoppedSession?.state)

            val resumeResult = repository.resumeSession()
            assertTrue(resumeResult.isFailure)

            val sessionAfterResume = repository.activeSession.value as? RunSession.ActiveSession
            assertEquals(RunState.STOPPED, sessionAfterResume?.state)

            val markMovingResult = repository.markMoving()
            assertTrue(markMovingResult.isSuccess)

            val movingSession = repository.activeSession.value as? RunSession.ActiveSession
            assertEquals(RunState.ACTIVE, movingSession?.state)
        }

    @Test
    fun pausePersonally_changesStateToPersonalPaused_andResumeSessionReactivatesIt() =
        runBlocking {
            val initializedSession =
                InitializedRunSession(
                    id = "test-session",
                    goal = RunGoal.THREE_KILOMETERS,
                    startMonotonicTimestamp = SystemClock.elapsedRealtime(),
                    startedAt = Clock.System.now(),
                )

            repository.attachActiveSession(initializedSession)

            val pauseResult = repository.pausePersonally()
            assertTrue(pauseResult.isSuccess)

            val pausedSession = repository.activeSession.value as? RunSession.ActiveSession
            assertEquals(RunState.PERSONAL_PAUSED, pausedSession?.state)

            val resumeResult = repository.resumeSession()
            assertTrue(resumeResult.isSuccess)

            val activeSession = repository.activeSession.value as? RunSession.ActiveSession
            assertEquals(RunState.ACTIVE, activeSession?.state)
        }

    @Test
    fun finishSession_capturesBrakesCountFromStopDetectionRepository() =
        runBlocking {
            val fakeStopRepo = FakeStopDetectionRepository()
            val repoWithStops =
                RunSessionRepositoryImpl(
                    locationClient = fakeLocationClient,
                    gpsPointFilterPolicy = GpsPointFilterPolicy(),
                    stopDetectionRepository = fakeStopRepo,
                    applicationScope = applicationScope,
                )

            val initializedSession =
                InitializedRunSession(
                    id = "brakes-test-session",
                    goal = RunGoal.FIVE_KILOMETERS,
                    startMonotonicTimestamp = SystemClock.elapsedRealtime(),
                    startedAt = Clock.System.now(),
                )

            repoWithStops.attachActiveSession(initializedSession)

            fakeStopRepo.setBrakesCount(2)

            val result = repoWithStops.finishSession()
            assertTrue(result.isSuccess)

            val summary = result.getOrNull()
            assertNotNull(summary)
            assertEquals(2, summary?.brakes)
            assertNull(summary?.regularity)

            val retrieved = repoWithStops.getFinishedSummary("brakes-test-session")
            assertNotNull(retrieved)
            assertEquals(2, retrieved?.brakes)

            repoWithStops.resetToIdle()
        }

    @Test
    fun locationUpdates_addRoutePointsAndCalculateDistance() =
        runBlocking {
            val initializedSession =
                InitializedRunSession(
                    id = "test-session",
                    goal = RunGoal.THREE_KILOMETERS,
                    startMonotonicTimestamp = SystemClock.elapsedRealtime(),
                    startedAt = Clock.System.now(),
                )

            repository.attachActiveSession(initializedSession)

            val firstTimestamp = SystemClock.elapsedRealtime()

            fakeLocationClient.sendLocation(
                RawLocationPoint(
                    latitude = -34.6037,
                    longitude = -58.3816,
                    accuracyMeters = 5f,
                    speedMetersPerSecond = 2f,
                    monotonicTimestampMillis = firstTimestamp,
                ),
            )

            withTimeout(3_000L.milliseconds) {
                repository.activeSession.first { session ->
                    session is RunSession.ActiveSession && session.routePoints.size == 1
                }
            }

            fakeLocationClient.sendLocation(
                RawLocationPoint(
                    latitude = -34.6027,
                    longitude = -58.3816,
                    accuracyMeters = 5f,
                    speedMetersPerSecond = 2f,
                    monotonicTimestampMillis = SystemClock.elapsedRealtime(),
                ),
            )

            val session =
                withTimeout(3_000L.milliseconds) {
                    repository.activeSession.first { s ->
                        s is RunSession.ActiveSession && s.routePoints.size == 2
                    } as RunSession.ActiveSession
                }

            assertEquals(2, session.routePoints.size)
            assertTrue(session.metrics.distanceMeters > 100.0)
        }

    @Test
    fun locationUpdates_ignoresPointsWithAccuracyGreaterThan30Meters() =
        runBlocking {
            val initializedSession =
                InitializedRunSession(
                    id = "test-session",
                    goal = RunGoal.THREE_KILOMETERS,
                    startMonotonicTimestamp = SystemClock.elapsedRealtime(),
                    startedAt = Clock.System.now(),
                )

            repository.attachActiveSession(initializedSession)

            fakeLocationClient.sendLocation(
                RawLocationPoint(
                    latitude = -34.6037,
                    longitude = -58.3816,
                    accuracyMeters = 35f,
                    speedMetersPerSecond = 2f,
                    monotonicTimestampMillis = SystemClock.elapsedRealtime(),
                ),
            )

            kotlinx.coroutines.delay(300L.milliseconds)

            val session = repository.activeSession.value as? RunSession.ActiveSession

            assertEquals(0, session?.routePoints?.size)
            assertEquals(0.0, session?.metrics?.distanceMeters ?: 0.0)
        }

    @Test
    fun locationUpdates_changesGpsQualityToNoSignalAfter15Seconds() =
        runBlocking {
            val initializedSession =
                InitializedRunSession(
                    id = "test-session",
                    goal = RunGoal.THREE_KILOMETERS,
                    startMonotonicTimestamp = SystemClock.elapsedRealtime(),
                    startedAt = Clock.System.now(),
                )

            repository.attachActiveSession(initializedSession)

            fakeLocationClient.sendSignalQuality(GpsQuality.FULL_SIGNAL)

            withTimeout(3_000L.milliseconds) {
                repository.activeSession.first { session ->
                    session is RunSession.ActiveSession && session.gpsQuality == GpsQuality.FULL_SIGNAL
                }
            }

            withTimeout(18_000L.milliseconds) {
                repository.activeSession.first { session ->
                    session is RunSession.ActiveSession && session.gpsQuality == GpsQuality.NO_SIGNAL
                }
            }

            val session = repository.activeSession.value as? RunSession.ActiveSession
            assertEquals(GpsQuality.NO_SIGNAL, session?.gpsQuality)
        }

    @Test
    fun pauseSession_keepsMetricsUnchanged() =
        runBlocking {
            val initializedSession =
                InitializedRunSession(
                    id = "test-session",
                    goal = RunGoal.THREE_KILOMETERS,
                    startMonotonicTimestamp = SystemClock.elapsedRealtime(),
                    startedAt = Clock.System.now(),
                )

            repository.attachActiveSession(initializedSession)

            fakeLocationClient.awaitLocationSubscriber()

            fakeLocationClient.sendLocation(
                RawLocationPoint(
                    latitude = -34.6037,
                    longitude = -58.3816,
                    accuracyMeters = 5f,
                    speedMetersPerSecond = 2f,
                    monotonicTimestampMillis = SystemClock.elapsedRealtime(),
                ),
            )

            withTimeout(3_000L.milliseconds) {
                repository.activeSession.first { session ->
                    session is RunSession.ActiveSession && session.routePoints.size == 1
                }
            }

            val pauseResult = repository.pauseSession()
            assertTrue(pauseResult.isSuccess)

            val activeSessionWhenPaused = repository.activeSession.value as? RunSession.ActiveSession
            val metricsWhenPaused = activeSessionWhenPaused?.metrics

            kotlinx.coroutines.delay(2_500L.milliseconds)

            val activeSessionAfterWaiting = repository.activeSession.value as? RunSession.ActiveSession
            val metricsAfterWaiting = activeSessionAfterWaiting?.metrics

            assertEquals(metricsWhenPaused, metricsAfterWaiting)
        }

    @Test
    fun resumeSession_continuesTrackingAndPreservesPreviousPoints() {
        runBlocking {
            val initializedSession =
                InitializedRunSession(
                    id = "test-session",
                    goal = RunGoal.THREE_KILOMETERS,
                    startMonotonicTimestamp = SystemClock.elapsedRealtime(),
                    startedAt = Clock.System.now(),
                )

            repository.attachActiveSession(initializedSession)

            fakeLocationClient.awaitLocationSubscriber()

            fakeLocationClient.sendLocation(
                RawLocationPoint(
                    latitude = -34.6037,
                    longitude = -58.3816,
                    accuracyMeters = 5f,
                    speedMetersPerSecond = 2f,
                    monotonicTimestampMillis = SystemClock.elapsedRealtime(),
                ),
            )

            withTimeout(3_000L.milliseconds) {
                repository.activeSession.first { session ->
                    session is RunSession.ActiveSession && session.routePoints.size == 1
                }
            }

            val pauseResult = repository.pauseSession()
            assertTrue(pauseResult.isSuccess)

            val pausedSession = repository.activeSession.value as? RunSession.ActiveSession
            val pausedDistance = pausedSession?.metrics?.distanceMeters ?: 0.0
            val pausedElapsedTime = pausedSession?.metrics?.elapsedTimeSeconds ?: 0L

            val resumeResult = repository.resumeSession()
            assertTrue(resumeResult.isSuccess)

            fakeLocationClient.awaitLocationSubscriber()

            fakeLocationClient.sendLocation(
                RawLocationPoint(
                    latitude = -34.6027,
                    longitude = -58.3816,
                    accuracyMeters = 5f,
                    speedMetersPerSecond = 2f,
                    monotonicTimestampMillis = SystemClock.elapsedRealtime(),
                ),
            )

            val updatedSession =
                withTimeoutOrNull(3_000L.milliseconds) {
                    repository.activeSession.first { session ->
                        session is RunSession.ActiveSession &&
                            session.routePoints.size == 2 &&
                            session.metrics.distanceMeters > pausedDistance
                    } as? RunSession.ActiveSession
                }

            assertNotNull(
                "No se agregó el segundo punto después de reanudar.",
                updatedSession,
            )

            assertEquals(2, updatedSession?.routePoints?.size)
            assertTrue((updatedSession?.metrics?.distanceMeters ?: 0.0) > pausedDistance)

            val updatedElapsedTime =
                withTimeoutOrNull(3_000L.milliseconds) {
                    repository.activeSession.first { session ->
                        session is RunSession.ActiveSession &&
                            session.metrics.elapsedTimeSeconds > pausedElapsedTime
                    }
                }

            assertNotNull(
                "El tiempo transcurrido no aumentó después de reanudar.",
                updatedElapsedTime,
            )
        }
    }

    @Test
    fun locationPermissionRevoked_doesNotAutoPauseSession() =
        runBlocking {
            val initializedSession =
                InitializedRunSession(
                    id = "test-session",
                    goal = RunGoal.THREE_KILOMETERS,
                    startMonotonicTimestamp = SystemClock.elapsedRealtime(),
                    startedAt = Clock.System.now(),
                )

            repository.attachActiveSession(initializedSession)

            val sessionBefore = repository.activeSession.value as? RunSession.ActiveSession
            assertNotNull(sessionBefore)
            assertEquals(RunState.ACTIVE, sessionBefore?.state)

            fakeLocationClient.triggerSecurityException()

            kotlinx.coroutines.delay(1_500L.milliseconds)

            val sessionAfter = repository.activeSession.value as? RunSession.ActiveSession
            assertNotNull(sessionAfter)
            assertEquals(RunState.ACTIVE, sessionAfter?.state)
            assertTrue(repository.isRunInProgress.value)
        }

    private class FakeStopDetectionRepository : StopDetectionRepository {
        private val _state = MutableStateFlow(StopDetectionState())
        override val state: StateFlow<StopDetectionState> = _state

        fun setBrakesCount(count: Int) {
            val fakeBrakes =
                List(count) { index ->
                    BrakeEvent(id = "brake-$index", location = null, detectedAtMillis = 1000L)
                }
            _state.value = _state.value.copy(brakes = fakeBrakes)
        }

        override fun classifyCurrentStop(kind: StopKind) {}
    }
}
