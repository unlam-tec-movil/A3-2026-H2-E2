package com.trazo.app.domain.repository

import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RunSession
import kotlinx.coroutines.flow.StateFlow

interface RunSessionRepository {
    val activeSession: StateFlow<RunSession>

    val isRunInProgress: StateFlow<Boolean>

    fun attachActiveSession(session: InitializedRunSession)

    suspend fun pauseSession(): Result<Unit>

    suspend fun pausePersonally(): Result<Unit>

    suspend fun markStopped(): Result<Unit>

    suspend fun markMoving(): Result<Unit>

    suspend fun resumeSession(): Result<Unit>

    suspend fun finishSession(): Result<FrozenRunSummary>

    fun resetToIdle()

    fun getFinishedSummary(id: String): FrozenRunSummary?

    /**
     * Elimina el resumen finalizado del almacén en memoria para el [id] provisto.
     *
     * El responsable de eliminar el resumen es save-run (HU-07), únicamente después
     * de persistir correctamente el recorrido.
     * HU-06 (Resultados) solamente consulta el resumen mediante [getFinishedSummary].
     * No se debe eliminar al finalizar el recorrido ni al navegar a Resultados.
     *
     * @return true si el resumen existía y fue removido, false en caso contrario.
     */
    fun removeFinishedSummary(id: String): Boolean
}
