package com.trazo.app.data.repository

import com.trazo.app.di.ApplicationScope
import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.isAccurate
import com.trazo.app.domain.repository.LocationClient
import com.trazo.app.domain.repository.MonotonicClock
import com.trazo.app.domain.repository.RunSessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * Guarda la sesión en curso y maneja el GPS y el reloj mientras está activa. Las reglas (transiciones,
 * distancia, tiempo, objetivo) son de [RunSession.ActiveSession]; acá solo se aplican con el mutex.
 */
@Singleton
class RunSessionRepositoryImpl
    @Inject
    constructor(
        private val locationClient: LocationClient,
        private val clock: MonotonicClock,
        @ApplicationScope private val applicationScope: CoroutineScope,
    ) : RunSessionRepository {
        private val mutex = Mutex()

        private val _activeSession = MutableStateFlow<RunSession>(RunSession.Idle)
        override val activeSession: StateFlow<RunSession> = _activeSession.asStateFlow()

        private val _isRunInProgress = MutableStateFlow(false)
        override val isRunInProgress: StateFlow<Boolean> = _isRunInProgress.asStateFlow()

        private val finishedSummaries = ConcurrentHashMap<String, FrozenRunSummary>()

        private var locationJob: Job? = null
        private var signalJob: Job? = null
        private var timerJob: Job? = null

        override fun attachActiveSession(session: InitializedRunSession) {
            if (_activeSession.value is RunSession.ActiveSession) return

            stopJobs()
            _activeSession.value = RunSession.ActiveSession.start(session, clock.nowMillis())
            _isRunInProgress.value = true
            startTracking()
        }

        override suspend fun pauseSession(): Result<Unit> = transition(resumesTracking = false) { it.pause(clock.nowMillis()) }

        override suspend fun pausePersonally(): Result<Unit> = transition(resumesTracking = false) { it.pausePersonally(clock.nowMillis()) }

        override suspend fun markStopped(): Result<Unit> = transition(resumesTracking = false) { it.markStopped(clock.nowMillis()) }

        override suspend fun markMoving(): Result<Unit> = transition(resumesTracking = true) { it.markMoving(clock.nowMillis()) }

        override suspend fun resumeSession(): Result<Unit> = transition(resumesTracking = true) { it.resume(clock.nowMillis()) }

        override suspend fun finishSession(): Result<FrozenRunSummary> =
            locked {
                val summary = requireActiveSession().toFrozenSummary(clock.nowMillis())
                stopJobs()
                finishedSummaries[summary.id] = summary
                _activeSession.value = RunSession.Idle
                _isRunInProgress.value = false
                summary
            }

        override fun resetToIdle() {
            stopJobs()
            _activeSession.value = RunSession.Idle
            _isRunInProgress.value = false
        }

        override fun getFinishedSummary(id: String): FrozenRunSummary? = finishedSummaries[id]

        /**
         * Aplica una transición de la sesión activa. Al pausar o detenerse corta el GPS y el reloj antes
         * de guardar; al volver a moverse los arranca después.
         */
        private suspend fun transition(
            resumesTracking: Boolean,
            change: (RunSession.ActiveSession) -> RunSession.ActiveSession,
        ): Result<Unit> =
            locked {
                val next = change(requireActiveSession())
                if (!resumesTracking) stopJobs()
                _activeSession.value = next
                if (resumesTracking) startTracking()
            }

        /** Ejecuta [block] con el mutex y convierte cualquier error (salvo la cancelación) en `Result.failure`. */
        private suspend fun <T> locked(block: () -> T): Result<T> =
            mutex.withLock {
                try {
                    Result.success(block())
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Result.failure(exception)
                }
            }

        /** Cambia la sesión activa, si la hay, dentro del mutex. */
        private suspend fun updateActive(change: (RunSession.ActiveSession) -> RunSession.ActiveSession) {
            mutex.withLock {
                val session = _activeSession.value as? RunSession.ActiveSession ?: return
                _activeSession.value = change(session)
            }
        }

        private fun startTracking() {
            locationJob?.cancel()
            locationJob =
                collectIgnoringErrors(locationClient.getLocationUpdates(intervalMillis = LOCATION_INTERVAL_MILLIS)) { point ->
                    processLocation(point)
                }

            signalJob?.cancel()
            signalJob =
                collectIgnoringErrors(locationClient.getSignalQualityUpdates()) { quality ->
                    updateActive { it.withGpsQuality(quality) }
                }

            timerJob?.cancel()
            timerJob =
                applicationScope.launch {
                    while (isActive) {
                        delay(TIMER_INTERVAL_MILLIS.milliseconds)
                        updateActive { session ->
                            val now = clock.nowMillis()
                            session.tick(now, session.isSignalLostAt(now))
                        }
                    }
                }
        }

        /** Recolecta [flow] en el scope de la app; un error del GPS corta ese flujo sin tirar la app. */
        private fun <T> collectIgnoringErrors(
            flow: Flow<T>,
            action: suspend (T) -> Unit,
        ): Job =
            applicationScope.launch {
                flow
                    .catch { exception -> if (exception is CancellationException) throw exception }
                    .collect { action(it) }
            }

        private suspend fun processLocation(point: RoutePoint) {
            if (!point.isAccurate()) return

            updateActive { it.addPoint(point, clock.nowMillis()) }
        }

        private fun requireActiveSession(): RunSession.ActiveSession =
            (_activeSession.value as? RunSession.ActiveSession)
                ?: throw IllegalStateException("No hay un recorrido activo.")

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
