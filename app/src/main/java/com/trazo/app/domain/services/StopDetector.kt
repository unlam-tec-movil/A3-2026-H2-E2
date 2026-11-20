package com.trazo.app.domain.services

import com.trazo.app.domain.model.stops.BrakeEvent
import com.trazo.app.domain.model.stops.ClosedStop
import com.trazo.app.domain.model.stops.QueryStatus
import com.trazo.app.domain.model.stops.StopDetectionConfig
import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopKind
import com.trazo.app.domain.model.stops.StopLocation
import com.trazo.app.domain.model.stops.StopPhase

data class Observation(
    val runId: String,
    val nowMillis: Long,
    val isStill: Boolean,
    val lastLocation: StopLocation?,
)

/** Resultado de un paso: el estado nuevo y, si en este paso terminó una parada, cuál fue. */
data class StopStep(
    val state: StopDetectionState,
    val closed: ClosedStop? = null,
)

/**
 * Reductor puro: sin reloj, sin E/S. El tiempo llega en cada observación.
 *
 * Una parada empieza a los 8 s de quietud (fase `Braking`) y se cierra una sola vez: cuando el
 * corredor contesta, cuando retoma sin contestar o cuando termina el recorrido.
 */
class StopDetector(
    private val config: StopDetectionConfig,
    private val idGenerator: () -> String,
) {
    fun initial(): StopDetectionState = StopDetectionState()

    /** El recorrido dejó de estar en curso: vuelve a Moving y cierra la parada sin contestar. */
    fun onRunStopped(state: StopDetectionState): StopStep = StopStep(StopDetectionState(StopPhase.Moving), closedWithoutAnswer(state.phase))

    fun advance(
        state: StopDetectionState,
        observation: Observation,
    ): StopStep =
        when (val phase = state.phase) {
            StopPhase.Moving ->
                if (observation.isStill) {
                    StopStep(state.copy(phase = StopPhase.Still(sinceMillis = observation.nowMillis)))
                } else {
                    StopStep(state)
                }

            is StopPhase.Still ->
                when {
                    !observation.isStill ->
                        StopStep(
                            resumeOrHold(state, phase.movingSinceMillis, observation.nowMillis) {
                                phase.copy(movingSinceMillis = it)
                            },
                        )

                    observation.nowMillis - phase.sinceMillis >= config.brakeAfterMillis ->
                        StopStep(startBrake(state, phase, observation))

                    else -> StopStep(state.copy(phase = phase.copy(movingSinceMillis = null)))
                }

            is StopPhase.Braking ->
                if (!observation.isStill) {
                    val next =
                        resumeOrHold(state, phase.movingSinceMillis, observation.nowMillis) {
                            phase.copy(movingSinceMillis = it)
                        }
                    val resumed = next.phase == StopPhase.Moving
                    StopStep(next, if (resumed) closedWithoutAnswer(phase) else null)
                } else {
                    val due =
                        phase.query == QueryStatus.NOT_DUE &&
                            observation.nowMillis - phase.sinceMillis > config.classifyAfterMillis
                    StopStep(
                        state.copy(
                            phase =
                                phase.copy(
                                    query = if (due) QueryStatus.PENDING else phase.query,
                                    movingSinceMillis = null,
                                ),
                        ),
                    )
                }

            is StopPhase.PersonalPause ->
                if (!observation.isStill) {
                    StopStep(
                        resumeOrHold(state, phase.movingSinceMillis, observation.nowMillis) {
                            phase.copy(movingSinceMillis = it)
                        },
                    )
                } else {
                    StopStep(state.copy(phase = phase.copy(movingSinceMillis = null)))
                }
        }

    /** Respuesta del corredor: cierra la parada con esa respuesta. Sin consulta pendiente no hace nada. */
    fun classify(
        state: StopDetectionState,
        kind: StopKind,
    ): StopStep {
        val phase = state.phase as? StopPhase.Braking ?: return StopStep(state)
        if (phase.query != QueryStatus.PENDING) return StopStep(state)
        val next =
            when (kind) {
                StopKind.BRAKE -> phase.copy(query = QueryStatus.ANSWERED)
                StopKind.PERSONAL_PAUSE ->
                    StopPhase.PersonalPause(
                        sinceMillis = phase.sinceMillis,
                        movingSinceMillis = phase.movingSinceMillis,
                    )
            }
        return StopStep(state.copy(phase = next), ClosedStop(phase.brake, kind))
    }

    /** Una parada ya contestada no se vuelve a cerrar, así no se guarda dos veces. */
    private fun closedWithoutAnswer(phase: StopPhase): ClosedStop? =
        (phase as? StopPhase.Braking)
            ?.takeIf { it.query != QueryStatus.ANSWERED }
            ?.let { ClosedStop(it.brake, answer = null) }

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

    private fun startBrake(
        state: StopDetectionState,
        still: StopPhase.Still,
        observation: Observation,
    ): StopDetectionState {
        val brake =
            BrakeEvent(
                id = idGenerator(),
                runId = observation.runId,
                location = observation.lastLocation,
                detectedAtMillis = observation.nowMillis,
            )
        return state.copy(
            phase =
                StopPhase.Braking(
                    sinceMillis = still.sinceMillis,
                    brake = brake,
                    query = QueryStatus.NOT_DUE,
                ),
        )
    }
}
