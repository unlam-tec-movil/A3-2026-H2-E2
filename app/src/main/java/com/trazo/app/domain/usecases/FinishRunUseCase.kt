package com.trazo.app.domain.usecases

import com.trazo.app.data.repository.BrakeRepository
import com.trazo.app.data.repository.RunSessionRepository
import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.stops.StopPhase
import com.trazo.app.domain.services.RegularityMonitor
import com.trazo.app.domain.services.StopMonitor
import javax.inject.Inject

/**
 * Finaliza el recorrido en curso y congela su resumen con las frenadas y la regularidad del piso.
 *
 * Una frenada abierta al finalizar se cierra y se guarda recién después (al pasar la sesión a reposo,
 * por `StopMonitor.closedStops`), así que se cuenta desde el monitor. La candidata se lee antes que
 * las frenadas guardadas y se unen por id: si justo se guarda en el medio, no se cuenta dos veces.
 */
class FinishRunUseCase
    @Inject
    constructor(
        private val runSessionRepository: RunSessionRepository,
        private val brakeRepository: BrakeRepository,
        private val stopMonitor: StopMonitor,
        private val regularityMonitor: RegularityMonitor,
    ) {
        suspend operator fun invoke(): Result<FrozenRunSummary> {
            val runId =
                (runSessionRepository.activeSession.value as? RunSession.ActiveSession)?.id
                    ?: return Result.failure(IllegalStateException("No hay un recorrido activo."))

            val openBrake =
                (stopMonitor.state.value.phase as? StopPhase.Braking)
                    ?.brake
                    ?.takeIf { it.runId == runId }
            val savedBrakes = brakeRepository.brakes.value.filter { it.runId == runId }
            val brakes =
                (savedBrakes + listOfNotNull(openBrake))
                    .distinctBy { it.id }
                    .sortedBy { it.detectedAtMillis }

            val regularityPercent =
                regularityMonitor.state.value
                    .takeIf { it.runId == runId }
                    ?.percentage

            return runSessionRepository.finishSession(brakes, regularityPercent)
        }
    }
