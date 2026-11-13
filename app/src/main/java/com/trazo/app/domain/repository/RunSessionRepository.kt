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
}
