package com.trazo.app.domain.repository

import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.model.regularity.ActiveRunSnapshot
import com.trazo.app.domain.model.stops.StopLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Estados en los que corre la detección de paradas: activo, detenido o en pausa personal. */
private val STOP_DETECTION_STATES = setOf(RunState.ACTIVE, RunState.STOPPED, RunState.PERSONAL_PAUSED)

/**
 * Id del recorrido mientras la detección de paradas aplica (activo, detenido o en pausa personal);
 * `null` en reposo, pausa manual o recorrido finalizado.
 */
val RunSessionRepository.runningRunId: Flow<String?>
    get() =
        activeSession
            .map { session ->
                (session as? RunSession.ActiveSession)
                    ?.takeIf { it.state in STOP_DETECTION_STATES }
                    ?.id
            }.distinctUntilChanged()

/** Recorrido en curso con su id y estado; `null` si la sesión está inactiva. Solo lee. */
val RunSessionRepository.activeRunSnapshot: Flow<ActiveRunSnapshot?>
    get() =
        activeSession.map { session ->
            when (session) {
                is RunSession.ActiveSession -> ActiveRunSnapshot(session.id, session.state)
                RunSession.Idle -> null
            }
        }

/** Ubicación de la frenada: el último punto de la traza de la sesión activa; `null` si no hay. */
fun RunSessionRepository.lastKnownStopLocation(): StopLocation? =
    (activeSession.value as? RunSession.ActiveSession)
        ?.routePoints
        ?.lastOrNull()
        ?.let { StopLocation(it.latitude, it.longitude, it.accuracyMeters) }

/**
 * Publica el estado de detección [state] del recorrido [runId]. Solo actúa si la sesión activa tiene
 * ese id y el cambio es una de las traducciones acordadas (D1-D3); en cualquier otro caso (incluida
 * la pausa manual) devuelve `Result.failure` sin llamar al repositorio.
 */
suspend fun RunSessionRepository.publishStopState(
    runId: String,
    state: RunState,
): Result<Unit> {
    val session =
        activeSession.value as? RunSession.ActiveSession
            ?: return rejected("No hay un recorrido activo.")
    if (session.id != runId) return rejected("El recorrido cambió.")

    return when (session.state to state) {
        RunState.ACTIVE to RunState.STOPPED -> markStopped()
        RunState.STOPPED to RunState.ACTIVE,
        RunState.PERSONAL_PAUSED to RunState.ACTIVE,
        -> markMoving()
        RunState.STOPPED to RunState.PERSONAL_PAUSED -> pausePersonally()
        else -> rejected("Cambio de ${session.state} a $state no soportado.")
    }
}

private fun rejected(message: String): Result<Unit> = Result.failure(IllegalStateException(message))
