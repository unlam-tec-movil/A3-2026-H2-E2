package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.repository.RunSessionRepository
import com.trazo.app.domain.repository.RunningRunSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Adapta la sesión de HU-04: entrega el id mientras la detección aplica (activa, detenida o en
 * pausa personal) y `null` en reposo, pausa manual o recorrido finalizado.
 */
class RunSessionRunningRunSource
    @Inject
    constructor(
        runSessionRepository: RunSessionRepository,
    ) : RunningRunSource {
        override val runningRunId: Flow<String?> =
            runSessionRepository.activeSession
                .map { session ->
                    (session as? RunSession.ActiveSession)
                        ?.takeIf { it.state in DETECTION_STATES }
                        ?.id
                }.distinctUntilChanged()

        private companion object {
            val DETECTION_STATES = setOf(RunState.ACTIVE, RunState.STOPPED, RunState.PERSONAL_PAUSED)
        }
    }
