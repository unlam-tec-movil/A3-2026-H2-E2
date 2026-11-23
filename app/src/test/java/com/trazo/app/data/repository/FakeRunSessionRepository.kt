package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.model.stops.BrakeEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

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

    /** Estados que dejaron `markStopped`, `markMoving` y `pausePersonally` cuando tuvieron éxito, en orden. */
    val appliedStates = mutableListOf<RunState>()

    /** Cuántas de las próximas llamadas a esas tres transiciones se rechazan con `Result.failure`. */
    var failNextTransitions = 0

    fun setSession(session: RunSession) {
        sessionStateFlow.value = session
        inProgressStateFlow.value = session is RunSession.ActiveSession
    }

    /** Pone una sesión activa con [id] y [state]; [routePoints] es la traza (el último punto es la ubicación). */
    fun setActive(
        id: String,
        state: RunState = RunState.ACTIVE,
        routePoints: List<RoutePoint> = emptyList(),
    ) {
        setSession(
            RunSession.ActiveSession(
                id = id,
                goal = RunGoal.FIVE_KILOMETERS,
                state = state,
                routePoints = routePoints,
                startMonotonicTimestamp = 0L,
                startedAt = Instant.fromEpochMilliseconds(0L),
            ),
        )
    }

    /**
     * Aplica una transición con las reglas reales de [RunSession.ActiveSession], para que el fake no
     * acepte orígenes que el modelo rechaza. Un origen inválido devuelve `Result.failure`.
     */
    private fun modelTransition(change: (RunSession.ActiveSession) -> RunSession.ActiveSession): Result<RunState> {
        val current =
            sessionStateFlow.value as? RunSession.ActiveSession
                ?: return Result.failure(IllegalStateException("No active run"))
        return runCatching { change(current) }.map { next ->
            sessionStateFlow.value = next
            next.state
        }
    }

    /** Aplica una transición de detección, salvo que toque rechazarla; registra el estado si tuvo éxito. */
    private fun detectionTransition(change: (RunSession.ActiveSession) -> RunSession.ActiveSession): Result<Unit> {
        if (failNextTransitions > 0) {
            failNextTransitions--
            return Result.failure(IllegalStateException("Rechazo simulado"))
        }
        return modelTransition(change).map { newState -> appliedStates += newState }
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

    override suspend fun pauseSession(): Result<Unit> = modelTransition { it.pause(NOW_MILLIS) }.map { }

    override suspend fun pausePersonally(): Result<Unit> = detectionTransition { it.pausePersonally(NOW_MILLIS) }

    override suspend fun markStopped(): Result<Unit> = detectionTransition { it.markStopped(NOW_MILLIS) }

    override suspend fun markMoving(): Result<Unit> = detectionTransition { it.markMoving(NOW_MILLIS) }

    override suspend fun resumeSession(): Result<Unit> = modelTransition { it.resume(NOW_MILLIS) }.map { }

    /** Si es verdadero, la próxima llamada a [finishSession] falla sin tocar la sesión. */
    var failNextFinish = false

    override suspend fun finishSession(
        brakes: List<BrakeEvent>?,
        regularityPercent: Int?,
    ): Result<FrozenRunSummary> {
        if (failNextFinish) {
            failNextFinish = false
            return Result.failure(IllegalStateException("Falla simulada"))
        }
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
                brakes = brakes,
                regularityPercent = regularityPercent,
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

    override fun removeFinishedSummary(id: String): Boolean = summaries.remove(id) != null

    private companion object {
        /** Reloj fijo de las transiciones: el fake no mide tiempo. */
        const val NOW_MILLIS = 0L
    }
}
