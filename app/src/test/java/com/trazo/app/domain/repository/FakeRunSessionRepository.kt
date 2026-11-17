package com.trazo.app.domain.repository

import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake en memoria de [RunSessionRepository] para tests JVM.
 * Compartido entre los tests del mapa y los del resultado del recorrido.
 */
class FakeRunSessionRepository : RunSessionRepository {
    private val sessionStateFlow = MutableStateFlow<RunSession>(RunSession.Idle)
    override val activeSession: StateFlow<RunSession> = sessionStateFlow

    private val inProgressStateFlow = MutableStateFlow(false)
    override val isRunInProgress: StateFlow<Boolean> = inProgressStateFlow

    private val summaries = HashMap<String, FrozenRunSummary>()

    private val summaryLookups = mutableListOf<String>()

    /** Ids consultados con [getFinishedSummary], en orden de llamada. */
    val getFinishedSummaryCalls: List<String> get() = summaryLookups

    /** Precarga un resumen para que [getFinishedSummary] lo devuelva. */
    fun putFinishedSummary(summary: FrozenRunSummary) {
        summaries[summary.id] = summary
    }

    fun setSession(session: RunSession) {
        sessionStateFlow.value = session
        inProgressStateFlow.value = session is RunSession.ActiveSession
    }

    override fun attachActiveSession(session: InitializedRunSession) {
        setSession(
            RunSession.ActiveSession(
                id = session.id,
                goal = session.goal,
                state = RunState.ACTIVE,
                startMonotonicTimestamp = session.startMonotonicTimestamp,
                startedAt = session.startedAt,
                isDemoMode = session.isDemoMode,
            ),
        )
    }

    override suspend fun pauseSession(): Result<Unit> {
        val current =
            sessionStateFlow.value as? RunSession.ActiveSession
                ?: return Result.failure(IllegalStateException("No active run"))
        sessionStateFlow.value = current.copy(state = RunState.PAUSED)
        return Result.success(Unit)
    }

    override suspend fun pausePersonally(): Result<Unit> {
        val current =
            sessionStateFlow.value as? RunSession.ActiveSession
                ?: return Result.failure(IllegalStateException("No active run"))
        sessionStateFlow.value = current.copy(state = RunState.PERSONAL_PAUSED)
        return Result.success(Unit)
    }

    override suspend fun markStopped(): Result<Unit> {
        val current =
            sessionStateFlow.value as? RunSession.ActiveSession
                ?: return Result.failure(IllegalStateException("No active run"))
        sessionStateFlow.value = current.copy(state = RunState.STOPPED)
        return Result.success(Unit)
    }

    override suspend fun markMoving(): Result<Unit> {
        val current =
            sessionStateFlow.value as? RunSession.ActiveSession
                ?: return Result.failure(IllegalStateException("No active run"))
        if (current.state != RunState.STOPPED) {
            return Result.failure(IllegalStateException("Not stopped"))
        }
        sessionStateFlow.value = current.copy(state = RunState.ACTIVE)
        return Result.success(Unit)
    }

    override suspend fun resumeSession(): Result<Unit> {
        val current =
            sessionStateFlow.value as? RunSession.ActiveSession
                ?: return Result.failure(IllegalStateException("No active run"))
        if (current.state != RunState.PAUSED && current.state != RunState.PERSONAL_PAUSED) {
            return Result.failure(IllegalStateException("Cannot manually resume"))
        }
        sessionStateFlow.value = current.copy(state = RunState.ACTIVE)
        return Result.success(Unit)
    }

    override suspend fun finishSession(): Result<FrozenRunSummary> {
        val current =
            sessionStateFlow.value as? RunSession.ActiveSession
                ?: return Result.failure(IllegalStateException("No active run"))

        val summary =
            FrozenRunSummary(
                id = current.id,
                goal = current.goal,
                routePoints = current.routePoints,
                metrics = current.metrics,
                startedAt = current.startedAt,
                startedAtMonotonicTimestamp = current.startMonotonicTimestamp,
                finishedAtMonotonicTimestamp = 2000L,
                goalReached = current.goalReached,
                isDemoMode = current.isDemoMode,
            )

        summaries[summary.id] = summary
        resetToIdle()

        return Result.success(summary)
    }

    override fun resetToIdle() {
        sessionStateFlow.value = RunSession.Idle
        inProgressStateFlow.value = false
    }

    override fun getFinishedSummary(id: String): FrozenRunSummary? {
        summaryLookups += id
        return summaries[id]
    }
}
