package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.model.regularity.RegularityConfig
import com.trazo.app.domain.model.regularity.RegularityState
import com.trazo.app.domain.model.regularity.SegmentClass
import com.trazo.app.domain.repository.MonotonicClock
import com.trazo.app.domain.repository.MotionSensorAvailability
import com.trazo.app.domain.repository.MotionSource
import com.trazo.app.domain.repository.RegularityMonitor
import com.trazo.app.domain.repository.RunSessionRepository
import com.trazo.app.domain.repository.activeRunSnapshot
import com.trazo.app.domain.services.RegularityMeter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Orquesta estado del recorrido, sensor y reloj para publicar la regularidad. Solo lee el sensor con
 * el recorrido en [RunState.ACTIVE]; cualquier otro estado descarta el tramo abierto y conserva los
 * cerrados. El valor se conserva al terminar el recorrido, hasta que empiece otro.
 */
class SensorRegularityMonitor(
    private val motionSource: MotionSource,
    private val runSessionRepository: RunSessionRepository,
    availability: MotionSensorAvailability,
    private val clock: MonotonicClock,
    scope: CoroutineScope,
    config: RegularityConfig = RegularityConfig(),
) : RegularityMonitor {
    private val meter = RegularityMeter(config)
    private val _state = MutableStateFlow(RegularityState(sensorAvailable = availability.isAvailable()))
    override val state: StateFlow<RegularityState> = _state.asStateFlow()

    private var lastRunId: String? = null

    init {
        scope.launch {
            runSessionRepository.activeRunSnapshot.distinctUntilChanged().collectLatest { snapshot ->
                meter.reset()
                if (snapshot == null) return@collectLatest
                if (snapshot.runId != lastRunId) {
                    lastRunId = snapshot.runId
                    _state.update { RegularityState(runId = snapshot.runId, sensorAvailable = it.sensorAvailable) }
                }
                if (snapshot.state == RunState.ACTIVE) measure()
            }
        }
    }

    private suspend fun measure() {
        motionSource.samples().collect { sample ->
            val closed = meter.accept(clock.nowMillis(), sample.magnitude) ?: return@collect
            _state.update {
                it.copy(
                    completedSegments = it.completedSegments + 1,
                    lowVibrationSegments =
                        it.lowVibrationSegments + if (closed == SegmentClass.LOW_VIBRATION) 1 else 0,
                )
            }
        }
    }
}
