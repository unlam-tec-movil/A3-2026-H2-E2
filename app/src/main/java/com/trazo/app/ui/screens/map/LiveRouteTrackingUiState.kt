package com.trazo.app.ui.screens.map

import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunState

data class LiveRouteTrackingUiState(
    val hasActiveRun: Boolean = false,
    val isRunInProgress: Boolean = false,
    val runState: RunState = RunState.IDLE,
    val goal: RunGoal? = null,
    val formattedTime: String = "00:00:00",
    val formattedDistance: String = "0",
    val distanceUnit: String = "km",
    val formattedSpeed: String = "0",
    val speedUnit: String = "km/h",
    val formattedAverageSpeed: String = "0",
    val routePoints: List<RoutePoint> = emptyList(),
    val runnerPosition: RoutePoint? = null,
    val isMapCenteredOnRunner: Boolean = true,
    val gpsQuality: GpsQuality = GpsQuality.NO_SIGNAL,
    val goalReached: Boolean = false,
    val showFinishConfirmDialog: Boolean = false,
    val showShortRunDialog: Boolean = false,
    val showGoalCelebration: Boolean = false,
    val areControlActionsAllowed: Boolean = false,
    val rawDistanceMeters: Double = 0.0,
) {
    val isRunning: Boolean
        get() = runState == RunState.ACTIVE

    val isPaused: Boolean
        get() = runState == RunState.PAUSED || runState == RunState.PERSONAL_PAUSED

    val isStopped: Boolean
        get() = runState == RunState.STOPPED

    val canPause: Boolean
        get() = isRunning && areControlActionsAllowed

    val canResume: Boolean
        get() = isPaused && areControlActionsAllowed

    val canFinish: Boolean
        get() = (isRunning || isPaused || isStopped) && areControlActionsAllowed

    val suppressed: Boolean
        get() = showGoalCelebration || showFinishConfirmDialog || showShortRunDialog
}
