package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.stops.BrakeEvent
import com.trazo.app.domain.model.stops.QueryStatus
import com.trazo.app.domain.model.stops.StopDetectionConfig
import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopLocation
import com.trazo.app.domain.model.stops.StopPhase

data class Observation(
    val nowMillis: Long,
    val isStill: Boolean,
    val lastLocation: StopLocation?,
)

/** Reductor puro: sin reloj, sin E/S. El tiempo llega en cada observación. */
class StopDetector(
    private val config: StopDetectionConfig,
    private val idGenerator: () -> String,
) {
    fun initial(): StopDetectionState = StopDetectionState()

    fun onRunStopped(state: StopDetectionState): StopDetectionState = state.copy(phase = StopPhase.Moving)

    fun advance(
        state: StopDetectionState,
        observation: Observation,
    ): StopDetectionState =
        when (val phase = state.phase) {
            StopPhase.Moving ->
                if (observation.isStill) {
                    state.copy(phase = StopPhase.Still(sinceMillis = observation.nowMillis))
                } else {
                    state
                }

            is StopPhase.Still ->
                when {
                    !observation.isStill -> state.copy(phase = StopPhase.Moving)
                    observation.nowMillis - phase.sinceMillis >= config.brakeAfterMillis ->
                        registerBrake(state, phase, observation)
                    else -> state
                }

            is StopPhase.Braking ->
                if (observation.isStill) state else state.copy(phase = StopPhase.Moving)

            is StopPhase.PersonalPause -> state
        }

    private fun registerBrake(
        state: StopDetectionState,
        still: StopPhase.Still,
        observation: Observation,
    ): StopDetectionState {
        val brake =
            BrakeEvent(
                id = idGenerator(),
                location = observation.lastLocation,
                detectedAtMillis = observation.nowMillis,
            )
        return state.copy(
            phase =
                StopPhase.Braking(
                    sinceMillis = still.sinceMillis,
                    brakeId = brake.id,
                    query = QueryStatus.NOT_DUE,
                ),
            brakes = state.brakes + brake,
        )
    }
}
