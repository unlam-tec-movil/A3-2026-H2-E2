package com.trazo.app.domain.services

import com.trazo.app.data.clock.MonotonicClock
import com.trazo.app.data.repository.RunSessionRepository
import com.trazo.app.data.sensors.MotionSource
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.model.stops.ClosedStop
import com.trazo.app.domain.model.stops.StopDetectionConfig
import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopKind
import com.trazo.app.domain.model.stops.StopLocation
import com.trazo.app.domain.model.stops.StopPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/**
 * Orquesta sensor, reloj y recorrido en curso. Solo lee el sensor mientras hay un recorrido en
 * curso; cada observación usa el reloj monotónico y un tick periódico. Cuando una parada termina, la
 * emite en [closedStops]; no decide si se guarda.
 */
class SensorStopMonitor(
    private val motionSource: MotionSource,
    private val runSessionRepository: RunSessionRepository,
    private val clock: MonotonicClock,
    scope: CoroutineScope,
    private val config: StopDetectionConfig = StopDetectionConfig(),
    idGenerator: () -> String = { UUID.randomUUID().toString() },
) : StopMonitor {
    private val detector = StopDetector(config, idGenerator)
    private val _state = MutableStateFlow(detector.initial())
    override val state: StateFlow<StopDetectionState> = _state.asStateFlow()

    // Con buffer para que `applyStep` (no suspende) no pierda paradas si el suscriptor está ocupado.
    private val _closedStops = MutableSharedFlow<ClosedStop>(extraBufferCapacity = CLOSED_STOPS_BUFFER)
    override val closedStops: Flow<ClosedStop> = _closedStops.asSharedFlow()

    /** Última lectura del estimador y el instante (reloj monotónico) en que llegó. */
    private data class Reading(
        val isStill: Boolean,
        val atMillis: Long,
    )

    init {
        scope.launch {
            runSessionRepository.runningRunId.distinctUntilChanged().collectLatest { runId ->
                if (runId == null) {
                    applyStep { detector.onRunStopped(it) }
                } else {
                    track(runId)
                }
            }
        }
    }

    override fun answer(kind: StopKind) {
        applyStep { detector.classify(it, kind) }
    }

    /**
     * Aplica un paso del detector. Los ticks y la respuesta del corredor pueden llegar a la vez: se
     * reintenta hasta escribir sobre el estado leído, y la parada cerrada se emite una sola vez.
     */
    private fun applyStep(transition: (StopDetectionState) -> StopStep) {
        while (true) {
            val current = _state.value
            val step = transition(current)
            if (_state.compareAndSet(current, step.state)) {
                step.closed?.let { _closedStops.tryEmit(it) }
                return
            }
        }
    }

    private suspend fun track(runId: String) {
        // Si se pasó de un recorrido a otro sin pausa, cierra la parada que haya quedado abierta.
        applyStep { detector.onRunStopped(it) }
        // Al iniciar o reanudar el recorrido HU-04 está en ACTIVE: es lo último publicado.
        var lastPublished: RunState? = RunState.ACTIVE
        val estimator = StillnessEstimator(config)
        val reading = AtomicReference<Reading?>(null)

        coroutineScope {
            launch {
                motionSource.samples().collect { sample ->
                    reading.set(Reading(estimator.accept(sample), clock.nowMillis()))
                }
            }
            launch {
                while (isActive) {
                    delay(config.tickIntervalMillis)
                    observe(runId, reading.get())
                    // Si el repositorio rechaza, no se registra como publicado y se reintenta en el
                    // siguiente tick mientras la fase siga requiriendo ese estado.
                    val desired = _state.value.phase.toRunState()
                    if (desired != lastPublished && runSessionRepository.publishStopState(runId, desired).isSuccess) {
                        lastPublished = desired
                    }
                }
            }
        }
    }

    private fun observe(
        runId: String,
        reading: Reading?,
    ) {
        val now = clock.nowMillis()
        // Sin muestras recientes no hay evidencia de quietud: se trata como «en movimiento».
        val isStill = reading != null && reading.isStill && now - reading.atMillis <= config.sampleStaleMillis
        val observation = Observation(runId, now, isStill, runSessionRepository.lastKnownStopLocation())
        applyStep { detector.advance(it, observation) }
    }

    private fun StopPhase.toRunState(): RunState =
        when (this) {
            StopPhase.Moving, is StopPhase.Still -> RunState.ACTIVE
            is StopPhase.Braking -> RunState.STOPPED
            is StopPhase.PersonalPause -> RunState.PERSONAL_PAUSED
        }

    private companion object {
        const val CLOSED_STOPS_BUFFER = 64
    }
}

/** Estados en los que corre la detección de paradas: activo, detenido o en pausa personal. */
private val STOP_DETECTION_STATES = setOf(RunState.ACTIVE, RunState.STOPPED, RunState.PERSONAL_PAUSED)

/**
 * Id del recorrido mientras la detección de paradas aplica (activo, detenido o en pausa personal);
 * `null` en reposo, pausa manual o recorrido finalizado.
 */
internal val RunSessionRepository.runningRunId: Flow<String?>
    get() =
        activeSession
            .map { session ->
                (session as? RunSession.ActiveSession)
                    ?.takeIf { it.state in STOP_DETECTION_STATES }
                    ?.id
            }.distinctUntilChanged()

/** Ubicación de la frenada: el último punto de la traza de la sesión activa; `null` si no hay. */
internal fun RunSessionRepository.lastKnownStopLocation(): StopLocation? =
    (activeSession.value as? RunSession.ActiveSession)
        ?.routePoints
        ?.lastOrNull()
        ?.let { StopLocation(it.latitude, it.longitude, it.accuracyMeters) }

/**
 * Publica el estado de detección [state] del recorrido [runId]. Solo actúa si la sesión activa tiene
 * ese id y el cambio es una de las traducciones acordadas (D1-D3); en cualquier otro caso (incluida
 * la pausa manual) devuelve `Result.failure` sin llamar al repositorio.
 */
internal suspend fun RunSessionRepository.publishStopState(
    runId: String,
    state: RunState,
): Result<Unit> {
    val session =
        activeSession.value as? RunSession.ActiveSession
            ?: return rejected("No hay un recorrido activo.")
    if (session.id != runId) return rejected("El recorrido cambió.")

    return when (session.state to state) {
        // El corredor ya reanudó a mano desde una pausa personal: no hay nada que publicar.
        RunState.ACTIVE to RunState.ACTIVE -> Result.success(Unit)
        RunState.ACTIVE to RunState.STOPPED -> markStopped()
        RunState.STOPPED to RunState.ACTIVE,
        RunState.PERSONAL_PAUSED to RunState.ACTIVE,
        -> markMoving()
        RunState.STOPPED to RunState.PERSONAL_PAUSED -> pausePersonally()
        else -> rejected("Cambio de ${session.state} a $state no soportado.")
    }
}

private fun rejected(message: String): Result<Unit> = Result.failure(IllegalStateException(message))
