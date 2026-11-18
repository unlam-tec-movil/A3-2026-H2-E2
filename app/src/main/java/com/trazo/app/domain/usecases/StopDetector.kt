package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.stops.BrakeEvent
import com.trazo.app.domain.model.stops.QueryStatus
import com.trazo.app.domain.model.stops.StopDetectionConfig
import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopKind
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
                    !observation.isStill ->
                        resumeOrHold(state, phase.movingSinceMillis, observation.nowMillis) {
                            phase.copy(movingSinceMillis = it)
                        }
                    observation.nowMillis - phase.sinceMillis >= config.brakeAfterMillis ->
                        registerBrake(state, phase, observation)
                    else -> state.copy(phase = phase.copy(movingSinceMillis = null))
                }

            is StopPhase.Braking ->
                if (!observation.isStill) {
                    resumeOrHold(state, phase.movingSinceMillis, observation.nowMillis) {
                        phase.copy(movingSinceMillis = it)
                    }
                } else {
                    val due =
                        phase.query == QueryStatus.NOT_DUE &&
                            observation.nowMillis - phase.sinceMillis > config.classifyAfterMillis
                    state.copy(
                        phase =
                            phase.copy(
                                query = if (due) QueryStatus.PENDING else phase.query,
                                movingSinceMillis = null,
                            ),
                    )
                }

            is StopPhase.PersonalPause ->
                if (!observation.isStill) {
                    resumeOrHold(state, phase.movingSinceMillis, observation.nowMillis) {
                        phase.copy(movingSinceMillis = it)
                    }
                } else {
                    state.copy(phase = phase.copy(movingSinceMillis = null))
                }
        }

    /** Responde a la clasificación del corredor; sin consulta pendiente no cambia nada. */
    fun classify(
        state: StopDetectionState,
        kind: StopKind,
    ): StopDetectionState {
        val phase = state.phase as? StopPhase.Braking ?: return state
        if (phase.query != QueryStatus.PENDING) return state
        return when (kind) {
            StopKind.BRAKE -> state.copy(phase = phase.copy(query = QueryStatus.ANSWERED))
            StopKind.PERSONAL_PAUSE ->
                state.copy(
                    phase =
                        StopPhase.PersonalPause(
                            sinceMillis = phase.sinceMillis,
                            movingSinceMillis = phase.movingSinceMillis,
                        ),
                    brakes = state.brakes.filterNot { it.id == phase.brakeId },
                )
        }
    }

    /** Salir de la quietud exige [StopDetectionConfig.resumeConfirmMillis] de movimiento continuo. */
    private fun resumeOrHold(
        state: StopDetectionState,
        movingSinceMillis: Long?,
        nowMillis: Long,
        withMovingSince: (Long) -> StopPhase,
    ): StopDetectionState {
        val since = movingSinceMillis ?: nowMillis
        return if (nowMillis - since >= config.resumeConfirmMillis) {
            state.copy(phase = StopPhase.Moving)
        } else {
            state.copy(phase = withMovingSince(since))
        }
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
