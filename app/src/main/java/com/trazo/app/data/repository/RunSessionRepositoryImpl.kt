package com.trazo.app.data.repository

import android.os.SystemClock
import com.trazo.app.di.ApplicationScope
import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RawLocationPoint
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunMetrics
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.repository.LocationClient
import com.trazo.app.domain.repository.RunSessionRepository
import com.trazo.app.domain.usecases.GpsPointFilterPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunSessionRepositoryImpl
    @Inject
    constructor(
        private val locationClient: LocationClient,
        private val gpsPointFilterPolicy: GpsPointFilterPolicy,
        @ApplicationScope private val applicationScope: CoroutineScope,
    ) : RunSessionRepository {
        private val mutex = Mutex()

        private val _activeSession = MutableStateFlow<RunSession?>(null)

        override val activeSession: StateFlow<RunSession?> =
            _activeSession.asStateFlow()

        private var locationJob: Job? = null
        private var signalJob: Job? = null
        private var timerJob: Job? = null

        override fun attachActiveSession(session: InitializedRunSession) {
            stopJobs()

            val now = SystemClock.elapsedRealtime()

            _activeSession.value =
                RunSession(
                    id = session.id,
                    goal = session.goal,
                    state = RunState.RUNNING,
                    routePoints = emptyList(),
                    metrics = RunMetrics(),
                    startMonotonicTimestamp = session.startMonotonicTimestamp,
                    lastValidLocationTimestamp = null,
                    accumulatedElapsedTimeMillis = 0L,
                    resumedAtMonotonicTimestamp = now,
                    gpsQuality = GpsQuality.NO_SIGNAL,
                )

            startTracking()
        }

        override suspend fun pauseSession(): Result<Unit> =
            mutex.withLock {
                try {
                    val session = requireActiveSession()

                    check(session.state == RunState.RUNNING) {
                        "El recorrido no está en curso."
                    }

                    val now = SystemClock.elapsedRealtime()
                    val elapsedTime = calculateElapsedTime(session, now)

                    stopJobs()

                    _activeSession.value =
                        session.copy(
                            state = RunState.PAUSED,
                            metrics =
                                session.metrics.copy(
                                    elapsedTimeSeconds = elapsedTime / 1_000L,
                                    averageSpeedKmh =
                                        calculateAverageSpeed(
                                            session.metrics.distanceMeters,
                                            elapsedTime,
                                        ),
                                ),
                            accumulatedElapsedTimeMillis = elapsedTime,
                            resumedAtMonotonicTimestamp = null,
                        )

                    Result.success(Unit)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Result.failure(exception)
                }
            }

        override suspend fun resumeSession(): Result<Unit> =
            mutex.withLock {
                try {
                    val session = requireActiveSession()

                    check(session.state == RunState.PAUSED) {
                        "El recorrido no está pausado."
                    }

                    val now = SystemClock.elapsedRealtime()

                    _activeSession.value =
                        session.copy(
                            state = RunState.RUNNING,
                            lastValidLocationTimestamp = null,
                            resumedAtMonotonicTimestamp = now,
                            gpsQuality = GpsQuality.NO_SIGNAL,
                        )

                    startTracking()

                    Result.success(Unit)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Result.failure(exception)
                }
            }

        override suspend fun finishSession(): Result<FrozenRunSummary> =
            mutex.withLock {
                try {
                    val session = requireActiveSession()

                    check(
                        session.state == RunState.RUNNING ||
                            session.state == RunState.PAUSED,
                    ) {
                        "No hay un recorrido que pueda finalizarse."
                    }

                    val now = SystemClock.elapsedRealtime()
                    val elapsedTime = calculateElapsedTime(session, now)

                    stopJobs()

                    val finalMetrics =
                        session.metrics.copy(
                            elapsedTimeSeconds = elapsedTime / 1_000L,
                            averageSpeedKmh =
                                calculateAverageSpeed(
                                    session.metrics.distanceMeters,
                                    elapsedTime,
                                ),
                        )

                    val summary =
                        FrozenRunSummary(
                            id = session.id,
                            goal = session.goal,
                            routePoints = session.routePoints.toList(),
                            metrics = finalMetrics,
                            startedAtMonotonicTimestamp =
                                session.startMonotonicTimestamp,
                            finishedAtMonotonicTimestamp = now,
                        )

                    _activeSession.value =
                        session.copy(
                            state = RunState.FINISHED,
                            metrics = finalMetrics,
                            accumulatedElapsedTimeMillis = elapsedTime,
                            resumedAtMonotonicTimestamp = null,
                        )

                    Result.success(summary)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Result.failure(exception)
                }
            }

        override fun resetToIdle() {
            stopJobs()
            _activeSession.value = null
        }

        private fun startTracking() {
            startLocationUpdates()
            startSignalUpdates()
            startTimer()
        }

        private fun startLocationUpdates() {
            locationJob?.cancel()

            locationJob =
                applicationScope.launch {
                    locationClient
                        .getLocationUpdates(
                            intervalMillis = LOCATION_INTERVAL_MILLIS,
                        ).catch { exception ->
                            if (exception is CancellationException) {
                                throw exception
                            }
                        }.collect { rawPoint ->
                            processLocation(rawPoint)
                        }
                }
        }

        private fun startSignalUpdates() {
            signalJob?.cancel()

            signalJob =
                applicationScope.launch {
                    locationClient
                        .getSignalQualityUpdates()
                        .catch { exception ->
                            if (exception is CancellationException) {
                                throw exception
                            }
                        }.collect { quality ->
                            mutex.withLock {
                                val session =
                                    _activeSession.value
                                        ?: return@withLock

                                if (session.state == RunState.RUNNING) {
                                    _activeSession.value =
                                        session.copy(
                                            gpsQuality = quality,
                                        )
                                }
                            }
                        }
                }
        }

        private suspend fun processLocation(rawPoint: RawLocationPoint) {
            val point = rawPoint.toRoutePoint()

            if (!gpsPointFilterPolicy.isValid(point)) {
                return
            }

            mutex.withLock {
                val session =
                    _activeSession.value
                        ?: return@withLock

                if (session.state != RunState.RUNNING) {
                    return@withLock
                }

                val previousPoint = session.routePoints.lastOrNull()

                val additionalDistance =
                    previousPoint?.let {
                        calculateDistanceMeters(it, point)
                    } ?: 0.0

                val totalDistance =
                    session.metrics.distanceMeters + additionalDistance

                val elapsedTime =
                    calculateElapsedTime(
                        session,
                        SystemClock.elapsedRealtime(),
                    )

                val currentSpeed =
                    rawPoint.speedMetersPerSecond
                        .coerceAtLeast(0f) * 3.6

                val metrics =
                    RunMetrics(
                        elapsedTimeSeconds = elapsedTime / 1_000L,
                        distanceMeters = totalDistance,
                        currentSpeedKmh = currentSpeed,
                        averageSpeedKmh =
                            calculateAverageSpeed(
                                totalDistance,
                                elapsedTime,
                            ),
                    )

                _activeSession.value =
                    session.copy(
                        routePoints = session.routePoints + point,
                        metrics = metrics,
                        lastValidLocationTimestamp =
                            rawPoint.monotonicTimestampMillis,
                    )
            }
        }

        private fun startTimer() {
            timerJob?.cancel()

            timerJob =
                applicationScope.launch {
                    while (isActive) {
                        delay(TIMER_INTERVAL_MILLIS)

                        mutex.withLock {
                            val session =
                                _activeSession.value
                                    ?: return@withLock

                            if (session.state != RunState.RUNNING) {
                                return@withLock
                            }

                            val now = SystemClock.elapsedRealtime()

                            val elapsedTime =
                                calculateElapsedTime(
                                    session,
                                    now,
                                )

                            val timeoutReference =
                                session.lastValidLocationTimestamp
                                    ?: session.resumedAtMonotonicTimestamp
                                    ?: now

                            val signalLost =
                                gpsPointFilterPolicy.isSignalLost(
                                    lastValidTimestampMillis = timeoutReference,
                                    currentTimestampMillis = now,
                                )

                            _activeSession.value =
                                session.copy(
                                    gpsQuality =
                                        if (signalLost) {
                                            GpsQuality.NO_SIGNAL
                                        } else {
                                            session.gpsQuality
                                        },
                                    metrics =
                                        session.metrics.copy(
                                            elapsedTimeSeconds = elapsedTime / 1_000L,
                                            averageSpeedKmh =
                                                calculateAverageSpeed(
                                                    session.metrics.distanceMeters,
                                                    elapsedTime,
                                                ),
                                        ),
                                )
                        }
                    }
                }
        }

        private fun calculateElapsedTime(
            session: RunSession,
            now: Long,
        ): Long {
            val currentSegment =
                session.resumedAtMonotonicTimestamp
                    ?.let { (now - it).coerceAtLeast(0L) }
                    ?: 0L

            return session.accumulatedElapsedTimeMillis +
                currentSegment
        }

        private fun calculateAverageSpeed(
            distanceMeters: Double,
            elapsedMillis: Long,
        ): Double {
            if (elapsedMillis <= 0L) return 0.0

            val elapsedHours = elapsedMillis / 3_600_000.0

            return (distanceMeters / 1_000.0) / elapsedHours
        }

        private fun calculateDistanceMeters(
            first: RoutePoint,
            second: RoutePoint,
        ): Double {
            val earthRadiusMeters = 6_371_000.0

            val latitudeDifference =
                Math.toRadians(
                    second.latitude - first.latitude,
                )

            val longitudeDifference =
                Math.toRadians(
                    second.longitude - first.longitude,
                )

            val firstLatitude = Math.toRadians(first.latitude)
            val secondLatitude = Math.toRadians(second.latitude)

            val a =
                Math.sin(latitudeDifference / 2).let {
                    it * it
                } +
                    Math.cos(firstLatitude) *
                    Math.cos(secondLatitude) *
                    Math.sin(longitudeDifference / 2).let {
                        it * it
                    }

            val centralAngle =
                2 *
                    Math.atan2(
                        Math.sqrt(a.coerceIn(0.0, 1.0)),
                        Math.sqrt((1.0 - a).coerceAtLeast(0.0)),
                    )

            return earthRadiusMeters * centralAngle
        }

        private fun RawLocationPoint.toRoutePoint(): RoutePoint =
            RoutePoint(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = accuracyMeters,
                speedMetersPerSecond = speedMetersPerSecond,
                timestampMillis = monotonicTimestampMillis,
            )

        private fun requireActiveSession(): RunSession =
            checkNotNull(_activeSession.value) {
                "No hay un recorrido activo."
            }

        private fun stopJobs() {
            locationJob?.cancel()
            locationJob = null

            signalJob?.cancel()
            signalJob = null

            timerJob?.cancel()
            timerJob = null
        }

        private companion object {
            const val LOCATION_INTERVAL_MILLIS = 1_000L
            const val TIMER_INTERVAL_MILLIS = 1_000L
        }
    }
