package com.trazo.app.data.repository

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.data.clock.MonotonicClock
import com.trazo.app.data.location.FakeLocationClient
import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.model.stops.BrakeEvent
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
                clock = MonotonicClock { SystemClock.elapsedRealtime() },
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
        // El colector de señal corre en otro hilo y puede aplicar el valor del GPS antes de leer la
        // sesión: con el GPS informando señal débil, el resultado es el mismo en cualquier orden.
        fakeLocationClient.sendSignalQuality(GpsQuality.WEAK_SIGNAL)

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
    fun finishSession_storesBrakesAndRegularityInTheSummary() =
        runBlocking {
            repository.attachActiveSession(
                InitializedRunSession(
                    id = "test-session",
                    goal = RunGoal.THREE_KILOMETERS,
                    startMonotonicTimestamp = SystemClock.elapsedRealtime(),
                    startedAt = Clock.System.now(),
                ),
            )

            val brakes =
                listOf("b1", "b2").map {
                    BrakeEvent(id = it, runId = "test-session", location = null, detectedAtMillis = 0L)
                }

            val result = repository.finishSession(brakes = brakes, regularityPercent = 80).getOrThrow()

            assertEquals(brakes, result.brakes)
            assertEquals(2, result.brakeCount)
            assertEquals(80, result.regularityPercent)
            assertEquals(result, repository.getFinishedSummary("test-session"))
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

    private fun accuratePoint(latitude: Double) =
        RoutePoint(
            latitude = latitude,
            longitude = -58.3816,
            accuracyMeters = 5f,
            speedMetersPerSecond = 0f,
            monotonicTimestampMillis = SystemClock.elapsedRealtime(),
        )

    private suspend fun awaitPoints(count: Int) {
        withTimeout(3_000L.milliseconds) {
            repository.activeSession.first { session ->
                session is RunSession.ActiveSession && session.routePoints.size == count
            }
        }
    }

    // stop-detection D3: en una frenada el GPS sigue registrando la traza.
    @Test
    fun markStopped_keepsRecordingGpsPoints() =
        runBlocking {
            repository.attachActiveSession(
                InitializedRunSession(
                    id = "test-session",
                    goal = RunGoal.THREE_KILOMETERS,
                    startMonotonicTimestamp = SystemClock.elapsedRealtime(),
                    startedAt = Clock.System.now(),
                ),
            )
            fakeLocationClient.awaitLocationSubscriber()

            assertTrue(repository.markStopped().isSuccess)
            fakeLocationClient.sendLocation(accuratePoint(-34.6037))

            awaitPoints(1)
            val session = repository.activeSession.value as? RunSession.ActiveSession
            assertEquals(RunState.STOPPED, session?.state)
        }

    // stop-detection D1 y D2: pausa personal desde una frenada corta el GPS; volver a moverse lo arranca.
    @Test
    fun pausePersonallyFromStopped_stopsGps_andMarkMovingRestartsIt() =
        runBlocking {
            repository.attachActiveSession(
                InitializedRunSession(
                    id = "test-session",
                    goal = RunGoal.THREE_KILOMETERS,
                    startMonotonicTimestamp = SystemClock.elapsedRealtime(),
                    startedAt = Clock.System.now(),
                ),
            )
            fakeLocationClient.awaitLocationSubscriber()
            assertTrue(repository.markStopped().isSuccess)

            assertTrue(repository.pausePersonally().isSuccess)
            assertEquals(RunState.PERSONAL_PAUSED, (repository.activeSession.value as? RunSession.ActiveSession)?.state)
            withTimeout(3_000L.milliseconds) { fakeLocationClient.awaitNoLocationSubscriber() }

            assertTrue(repository.markMoving().isSuccess)
            assertEquals(RunState.ACTIVE, (repository.activeSession.value as? RunSession.ActiveSession)?.state)
            withTimeout(3_000L.milliseconds) { fakeLocationClient.awaitLocationSubscriber() }
            fakeLocationClient.sendLocation(accuratePoint(-34.6037))
            awaitPoints(1)
        }

    @Test
    fun pauseSessionFromStopped_pausesTheRun() =
        runBlocking {
            repository.attachActiveSession(
                InitializedRunSession(
                    id = "test-session",
                    goal = RunGoal.THREE_KILOMETERS,
                    startMonotonicTimestamp = SystemClock.elapsedRealtime(),
                    startedAt = Clock.System.now(),
                ),
            )
            assertTrue(repository.markStopped().isSuccess)

            assertTrue(repository.pauseSession().isSuccess)

            assertEquals(RunState.PAUSED, (repository.activeSession.value as? RunSession.ActiveSession)?.state)
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
                RoutePoint(
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
                RoutePoint(
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
}
