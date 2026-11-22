package com.trazo.app.ui.screens.map

import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunState

/** Traza de un recorrido guardado que el mapa dibuja: el [id] del recorrido y sus [points]. */
data class SavedTraceUi(
    val id: String,
    val points: List<RoutePoint>,
)

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
    /** Falló el último intento de finalizar: el diálogo muestra el error y «Reintentar». */
    val finishFailed: Boolean = false,
    /** Hay un intento de finalizar en curso: los botones del diálogo quedan deshabilitados. */
    val isFinishing: Boolean = false,
    /**
     * Trazas de los recorridos guardados, del más reciente al más antiguo. Solo hay trazas sin recorrido en
     * curso y con ubicación precisa; no se mezclan con [routePoints], que es la traza en vivo.
     */
    val savedTraces: List<SavedTraceUi> = emptyList(),
) {
    val isRunning: Boolean
        get() = runState == RunState.ACTIVE

    val isPaused: Boolean
        get() = runState == RunState.PAUSED || runState == RunState.PERSONAL_PAUSED

    val isStopped: Boolean
        get() = runState == RunState.STOPPED

    /** En una frenada (STOPPED) el recorrido sigue en curso, así que también se puede pausar. */
    val canPause: Boolean
        get() = (isRunning || isStopped) && areControlActionsAllowed

    val canResume: Boolean
        get() = isPaused && areControlActionsAllowed

    val canFinish: Boolean
        get() = (isRunning || isPaused || isStopped) && areControlActionsAllowed

    val suppressed: Boolean
        get() = showGoalCelebration || showFinishConfirmDialog || showShortRunDialog
}
