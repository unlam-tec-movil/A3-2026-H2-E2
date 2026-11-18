package com.trazo.app.domain.repository

import com.trazo.app.domain.model.map.RunState

interface RunStatePublisher {
    /**
     * Publica ACTIVE, STOPPED o PERSONAL_PAUSED para el recorrido [runId].
     * Devuelve failure si el recorrido cambió o el origen no es válido.
     */
    suspend fun publish(
        runId: String,
        state: RunState,
    ): Result<Unit>
}
