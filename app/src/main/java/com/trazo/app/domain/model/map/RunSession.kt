package com.trazo.app.domain.model.map

import kotlin.time.Instant

sealed interface RunSession {
    data object Idle : RunSession

    data class ActiveSession(
        val id: String,
        val goal: RunGoal,
        val state: RunState,
        val routePoints: List<RoutePoint> = emptyList(),
        val metrics: RunMetrics = RunMetrics(),
        val startMonotonicTimestamp: Long,
        val startedAt: Instant,
        val lastValidLocationTimestamp: Long? = null,
        val accumulatedElapsedTimeMillis: Long = 0L,
        val resumedAtMonotonicTimestamp: Long? = null,
        val gpsQuality: GpsQuality = GpsQuality.NO_SIGNAL,
        val goalReached: Boolean = false,
    ) : RunSession
}
