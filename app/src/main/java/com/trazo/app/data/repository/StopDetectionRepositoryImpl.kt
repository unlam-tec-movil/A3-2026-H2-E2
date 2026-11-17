package com.trazo.app.data.repository

import com.trazo.app.domain.model.stops.StopDetectionConfig
import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopKind
import com.trazo.app.domain.repository.MonotonicClock
import com.trazo.app.domain.repository.MotionSource
import com.trazo.app.domain.repository.RunningRunSource
import com.trazo.app.domain.repository.StopDetectionRepository
import com.trazo.app.domain.repository.StopLocationProvider
import com.trazo.app.domain.usecases.Observation
import com.trazo.app.domain.usecases.StillnessEstimator
import com.trazo.app.domain.usecases.StopDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/**
 * Orquesta sensor, reloj y recorrido en curso. Solo lee el sensor mientras hay un recorrido en
 * curso; cada observación usa el reloj monotónico y un tick periódico.
 */
class StopDetectionRepositoryImpl(
    private val motionSource: MotionSource,
    private val runningRunSource: RunningRunSource,
    private val locationProvider: StopLocationProvider,
    private val clock: MonotonicClock,
    scope: CoroutineScope,
    private val config: StopDetectionConfig = StopDetectionConfig(),
    idGenerator: () -> String = { UUID.randomUUID().toString() },
) : StopDetectionRepository {
    private val detector = StopDetector(config, idGenerator)
    private val _state = MutableStateFlow(detector.initial())
    override val state: StateFlow<StopDetectionState> = _state.asStateFlow()

    private var lastRunId: String? = null

    /** Última lectura del estimador y el instante (reloj monotónico) en que llegó. */
    private data class Reading(
        val isStill: Boolean,
        val atMillis: Long,
    )

    init {
        scope.launch {
            runningRunSource.runningRunId.distinctUntilChanged().collectLatest { runId ->
                if (runId == null) {
                    _state.update { detector.onRunStopped(it) }
                } else {
                    track(runId)
                }
            }
        }
    }

    override fun classifyCurrentStop(kind: StopKind) {
        _state.update { detector.classify(it, kind) }
    }

    private suspend fun track(runId: String) {
        // Mismo id (reanudación): conserva las frenadas. Otro id: parte de cero.
        if (runId != lastRunId) {
            _state.value = detector.initial()
            lastRunId = runId
        }
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
                    observe(reading.get())
                }
            }
        }
    }

    private fun observe(reading: Reading?) {
        val now = clock.nowMillis()
        // Sin muestras recientes no hay evidencia de quietud: se trata como «en movimiento».
        val isStill = reading != null && reading.isStill && now - reading.atMillis <= config.sampleStaleMillis
        val observation = Observation(now, isStill, locationProvider.lastKnown())
        _state.update { detector.advance(it, observation) }
    }
}
