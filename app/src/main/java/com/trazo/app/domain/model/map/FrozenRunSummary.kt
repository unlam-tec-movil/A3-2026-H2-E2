package com.trazo.app.domain.model.map

data class FrozenRunSummary(
    val id: String,
    val goal: RunGoal,
    val routePoints: List<RoutePoint>,
    val metrics: RunMetrics,
    val startedAtMonotonicTimestamp: Long,
    val finishedAtMonotonicTimestamp: Long,
)
