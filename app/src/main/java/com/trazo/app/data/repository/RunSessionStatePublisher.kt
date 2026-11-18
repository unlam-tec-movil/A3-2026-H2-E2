package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.repository.RunSessionRepository
import com.trazo.app.domain.repository.RunStatePublisher
import javax.inject.Inject
import javax.inject.Provider

/**
 * Publica el estado de detección sobre `RunSessionRepository` (HU-04). Solo actúa si la sesión
 * activa tiene ese id y el cambio es una de las traducciones acordadas (D1-D3); en cualquier otro
 * caso (incluida la pausa manual) devuelve `Result.failure` sin llamar al repositorio.
 */
class RunSessionStatePublisher
    @Inject
    constructor(
        private val runSessionRepository: Provider<RunSessionRepository>,
    ) : RunStatePublisher {
        override suspend fun publish(
            runId: String,
            state: RunState,
        ): Result<Unit> {
            val repository = runSessionRepository.get()
            val session =
                repository.activeSession.value as? RunSession.ActiveSession
                    ?: return rejected("No hay un recorrido activo.")
            if (session.id != runId) return rejected("El recorrido cambió.")

            return when (session.state to state) {
                RunState.ACTIVE to RunState.STOPPED -> repository.markStopped()
                RunState.STOPPED to RunState.ACTIVE,
                RunState.PERSONAL_PAUSED to RunState.ACTIVE,
                -> repository.markMoving()
                RunState.STOPPED to RunState.PERSONAL_PAUSED -> repository.pausePersonally()
                else -> rejected("Cambio de ${session.state} a $state no soportado.")
            }
        }

        private fun rejected(message: String): Result<Unit> = Result.failure(IllegalStateException(message))
    }
