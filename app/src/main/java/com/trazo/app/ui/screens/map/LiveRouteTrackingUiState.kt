package com.trazo.app.ui.screens.map

import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.RawLocationPoint
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunState

data class LiveRouteTrackingUiState(
    val hasActiveRun: Boolean = false,
    val runState: RunState = RunState.IDLE,
    val goal: RunGoal? = null,
    val routePoints: List<RoutePoint> = emptyList(),
    val elapsedTimeSeconds: Long = 0L,
    val distanceMeters: Double = 0.0,
    val currentSpeedKmh: Double = 0.0,
    val averageSpeedKmh: Double = 0.0,
    val gpsQuality: GpsQuality = GpsQuality.NO_SIGNAL,
    val currentLocation: RawLocationPoint? = null,
) {
    val isRunning: Boolean
        get() = runState == RunState.RUNNING

    val isPaused: Boolean
        get() = runState == RunState.PAUSED

    val canPause: Boolean
        get() = isRunning

    val canResume: Boolean
        get() = isPaused

    val canFinish: Boolean
        get() = isRunning || isPaused
}
