package com.trazo.app.domain.model.map

import com.trazo.app.domain.model.stops.BrakeEvent
import kotlin.time.Instant

data class FrozenRunSummary(
    val id: String,
    val goal: RunGoal,
    val routePoints: List<RoutePoint>,
    val metrics: RunMetrics,
    val startedAt: Instant,
    val startedAtMonotonicTimestamp: Long,
    val finishedAtMonotonicTimestamp: Long,
    val goalReached: Boolean,
    val isDemoMode: Boolean = false,
    /** Frenadas del recorrido, incluida la que quedó abierta al finalizar. */
    val brakes: List<BrakeEvent> = emptyList(),
    /** Regularidad del piso, 0..100; `null` si no hubo tramos medidos o no hay sensor. */
    val regularityPercent: Int? = null,
) {
    val brakeCount: Int get() = brakes.size
}
