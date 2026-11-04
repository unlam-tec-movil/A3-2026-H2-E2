package com.trazo.app.domain.model.map

data class RunSession(
    val id: String,
    val goal: RunGoal,
    val state: RunState = RunState.IDLE,
    val routePoints: List<RoutePoint> = emptyList(),
    val metrics: RunMetrics = RunMetrics(),
    val startMonotonicTimestamp: Long,
    val lastValidLocationTimestamp: Long? = null,
    val accumulatedElapsedTimeMillis: Long = 0L,
    val resumedAtMonotonicTimestamp: Long? = null,
)
