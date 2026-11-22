package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.stops.BrakeEvent
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

    /** Cierra la sesión y guarda su resumen con las frenadas y la regularidad que recibe. */
    suspend fun finishSession(
        brakes: List<BrakeEvent>? = emptyList(),
        regularityPercent: Int? = null,
    ): Result<FrozenRunSummary>

    fun resetToIdle()

    fun getFinishedSummary(id: String): FrozenRunSummary?

    /**
     * Borra el resumen [id] de memoria. Lo llama `save-run` (HU-07) después de guardar o descartar el
     * recorrido; el Resultado (HU-06) solo lee. Devuelve si existía.
     */
    fun removeFinishedSummary(id: String): Boolean
}
