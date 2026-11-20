package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.model.stops.StopDetectionConfig
import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopKind
import com.trazo.app.domain.model.stops.StopPhase
import com.trazo.app.domain.repository.MonotonicClock
import com.trazo.app.domain.repository.MotionSource
import com.trazo.app.domain.repository.RunStatePublisher
import com.trazo.app.domain.repository.RunningRunSource
import com.trazo.app.domain.repository.StopLocationProvider
import com.trazo.app.domain.repository.StopMonitor
import com.trazo.app.domain.usecases.Observation
import com.trazo.app.domain.usecases.SaveStopUseCase
import com.trazo.app.domain.usecases.StillnessEstimator
import com.trazo.app.domain.usecases.StopDetector
import com.trazo.app.domain.usecases.StopStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/**
 * Orquesta sensor, reloj y recorrido en curso. Solo lee el sensor mientras hay un recorrido en
 * curso; cada observación usa el reloj monotónico y un tick periódico. Cuando una parada termina, se
 * la pasa a [SaveStopUseCase], que decide si se guarda.
 */
class SensorStopMonitor(
    private val motionSource: MotionSource,
    private val runningRunSource: RunningRunSource,
    private val locationProvider: StopLocationProvider,
    private val publisher: RunStatePublisher,
    private val clock: MonotonicClock,
    private val saveStop: SaveStopUseCase,
    scope: CoroutineScope,
    private val config: StopDetectionConfig = StopDetectionConfig(),
    idGenerator: () -> String = { UUID.randomUUID().toString() },
) : StopMonitor {
    private val detector = StopDetector(config, idGenerator)
    private val _state = MutableStateFlow(detector.initial())
    override val state: StateFlow<StopDetectionState> = _state.asStateFlow()

    /** Última lectura del estimador y el instante (reloj monotónico) en que llegó. */
    private data class Reading(
        val isStill: Boolean,
        val atMillis: Long,
    )

    init {
        scope.launch {
            runningRunSource.runningRunId.distinctUntilChanged().collectLatest { runId ->
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
     * reintenta hasta escribir sobre el estado leído, y la parada cerrada se guarda una sola vez.
     */
    private fun applyStep(transition: (StopDetectionState) -> StopStep) {
        while (true) {
            val current = _state.value
            val step = transition(current)
            if (_state.compareAndSet(current, step.state)) {
                step.closed?.let { saveStop(it) }
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
                    // Si el puerto rechaza, no se registra como publicado y se reintenta en el
                    // siguiente tick mientras la fase siga requiriendo ese estado.
                    val desired = _state.value.phase.toRunState()
                    if (desired != lastPublished && publisher.publish(runId, desired).isSuccess) {
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
        val observation = Observation(runId, now, isStill, locationProvider.lastKnown())
        applyStep { detector.advance(it, observation) }
    }

    private fun StopPhase.toRunState(): RunState =
        when (this) {
            StopPhase.Moving, is StopPhase.Still -> RunState.ACTIVE
            is StopPhase.Braking -> RunState.STOPPED
            is StopPhase.PersonalPause -> RunState.PERSONAL_PAUSED
        }
}
