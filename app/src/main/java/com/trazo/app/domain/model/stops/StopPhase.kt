package com.trazo.app.domain.model.stops

enum class QueryStatus { NOT_DUE, PENDING, ANSWERED }

sealed interface StopPhase {
    data object Moving : StopPhase

    data class Still(
        val sinceMillis: Long,
        val movingSinceMillis: Long? = null,
    ) : StopPhase

    data class Braking(
        val sinceMillis: Long,
        val brakeId: String,
        val query: QueryStatus,
        val movingSinceMillis: Long? = null,
    ) : StopPhase

    data class PersonalPause(
        val sinceMillis: Long,
        val movingSinceMillis: Long? = null,
    ) : StopPhase
}
