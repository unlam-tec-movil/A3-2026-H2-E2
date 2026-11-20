package com.trazo.app.domain.model.stops

enum class QueryStatus { NOT_DUE, PENDING, ANSWERED }

sealed interface StopPhase {
    data object Moving : StopPhase

    data class Still(
        val sinceMillis: Long,
        val movingSinceMillis: Long? = null,
    ) : StopPhase

    /** Quieto 8 s: [brake] es la frenada candidata, que se guarda recién al cerrar la parada. */
    data class Braking(
        val sinceMillis: Long,
        val brake: BrakeEvent,
        val query: QueryStatus,
        val movingSinceMillis: Long? = null,
    ) : StopPhase

    data class PersonalPause(
        val sinceMillis: Long,
        val movingSinceMillis: Long? = null,
    ) : StopPhase
}
