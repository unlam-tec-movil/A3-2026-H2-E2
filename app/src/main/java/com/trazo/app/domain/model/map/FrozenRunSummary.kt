package com.trazo.app.domain.model.map

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
)
