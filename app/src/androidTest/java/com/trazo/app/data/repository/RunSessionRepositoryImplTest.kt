package com.trazo.app.data.repository

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RawLocationPoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.usecases.GpsPointFilterPolicy
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

        val session = repository.activeSession.value

        assertNotNull(session)
        assertEquals(RunState.ACTIVE, session?.state)
        assertEquals("test-session", session?.id)
        assertEquals(RunGoal.THREE_KILOMETERS, session?.goal)
        assertEquals(GpsQuality.NO_SIGNAL, session?.gpsQuality)
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
            assertEquals(
                RunState.PAUSED,
                repository.activeSession.value?.state,
            )
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
            assertEquals(
                RunState.ACTIVE,
                repository.activeSession.value?.state,
            )
        }

    @Test
    fun finishSession_returnsFrozenSummary() =
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
            assertEquals(
                "test-session",
                result.getOrNull()?.id,
            )
            assertEquals(
                RunGoal.THREE_KILOMETERS,
                result.getOrNull()?.goal,
            )
            assertEquals(
                RunState.FINISHED,
                repository.activeSession.value?.state,
            )
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
                repository.activeSession.first {
                    it?.routePoints?.size == 1
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
                    repository.activeSession.first {
                        it?.routePoints?.size == 2
                    }
                }

            assertEquals(2, session?.routePoints?.size)
            assertTrue((session?.metrics?.distanceMeters ?: 0.0) > 100.0)
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

            // Esperamos a que el repositorio procese el flujo.
            kotlinx.coroutines.delay(300L.milliseconds)

            val session = repository.activeSession.value

            assertEquals(0, session?.routePoints?.size)
            assertEquals(0.0, session?.metrics?.distanceMeters)
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
                repository.activeSession.first {
                    it?.gpsQuality == GpsQuality.FULL_SIGNAL
                }
            }

            withTimeout(18_000L.milliseconds) {
                repository.activeSession.first {
                    it?.gpsQuality == GpsQuality.NO_SIGNAL
                }
            }

            assertEquals(
                GpsQuality.NO_SIGNAL,
                repository.activeSession.value?.gpsQuality,
            )
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
                repository.activeSession.first {
                    it?.routePoints?.size == 1
                }
            }

            val pauseResult = repository.pauseSession()
            assertTrue(pauseResult.isSuccess)

            val metricsWhenPaused = repository.activeSession.value?.metrics

            kotlinx.coroutines.delay(2_500L.milliseconds)

            val metricsAfterWaiting = repository.activeSession.value?.metrics

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
                repository.activeSession.first {
                    it?.routePoints?.size == 1
                }
            }

            val pauseResult = repository.pauseSession()
            assertTrue(pauseResult.isSuccess)

            val pausedSession = repository.activeSession.value
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
                    repository.activeSession.first {
                        it?.routePoints?.size == 2 &&
                            (it.metrics.distanceMeters > pausedDistance)
                    }
                }

            assertNotNull(
                "No se agregó el segundo punto después de reanudar.",
                updatedSession,
            )

            assertEquals(2, updatedSession?.routePoints?.size)
            assertTrue((updatedSession?.metrics?.distanceMeters ?: 0.0) > pausedDistance)

            val updatedElapsedTime =
                withTimeoutOrNull(3_000L.milliseconds) {
                    repository.activeSession.first {
                        (it?.metrics?.elapsedTimeSeconds ?: 0L) > pausedElapsedTime
                    }
                }

            assertNotNull(
                "El tiempo transcurrido no aumentó después de reanudar.",
                updatedElapsedTime,
            )
        }
    }
}
