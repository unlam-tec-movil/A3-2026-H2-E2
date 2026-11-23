package com.trazo.app.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.app.data.repository.LocationRepository
import com.trazo.app.data.repository.RunSessionRepository
import com.trazo.app.data.repository.SavedRunRepository
import com.trazo.app.domain.model.LocationStatus
import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.model.saved.mostRecentNear
import com.trazo.app.domain.services.RegularityMonitor
import com.trazo.app.domain.usecases.FinishRunUseCase
import com.trazo.app.ui.components.common.RunValueFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val formattedSpeed: String = "0",
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
    /** Falló el último intento de finalizar: el diálogo muestra el error y «Reintentar». */
    val finishFailed: Boolean = false,
    /** Hay un intento de finalizar en curso: los botones del diálogo quedan deshabilitados. */
    val isFinishing: Boolean = false,
    /**
     * Trazas de los recorridos guardados, del más reciente al más antiguo. Solo hay trazas sin recorrido en
     * curso y con ubicación precisa; no se mezclan con [routePoints], que es la traza en vivo.
     */
    val savedTraces: List<SavedTraceUi> = emptyList(),
    /** Hay un recorrido en curso y el dispositivo no tiene acelerómetro: no se miden frenadas ni regularidad. */
    val showMotionSensorMissing: Boolean = false,
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

@HiltViewModel
class LiveRouteTrackingViewModel
    @Inject
    constructor(
        private val runSessionRepository: RunSessionRepository,
        private val locationRepository: LocationRepository,
        private val finishRun: FinishRunUseCase,
        private val savedRunRepository: SavedRunRepository,
        private val regularityMonitor: RegularityMonitor,
    ) : ViewModel() {
        private val activeSessionFlow = runSessionRepository.activeSession

        private val isRunInProgressFlow = runSessionRepository.isRunInProgress

        private val showFinishConfirmDialogFlow = MutableStateFlow(false)
        private val showShortRunDialogFlow = MutableStateFlow(false)
        private val showGoalCelebrationFlow = MutableStateFlow(false)
        private val isMapCenteredOnRunnerFlow = MutableStateFlow(true)
        private val finishFailedFlow = MutableStateFlow(false)
        private val isFinishingFlow = MutableStateFlow(false)

        private val locationStatusFlow = MutableStateFlow(locationRepository.readLocationStatus())

        /** Recorridos cuyas trazas se dibujan: hay guardados, ningún recorrido en curso y ubicación precisa. */
        private val displayedRunsFlow =
            combine(
                savedRunRepository.runs,
                isRunInProgressFlow,
                locationStatusFlow,
            ) { runs, isRunInProgress, locationStatus ->
                if (isRunInProgress || locationStatus != LocationStatus.READY) emptyList() else runs
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        private val savedTracesFlow =
            displayedRunsFlow.map { runs -> runs.map { SavedTraceUi(id = it.id, points = it.routePoints) } }

        private val _uiEffect = Channel<LiveRouteTrackingUiEffect>(Channel.BUFFERED)

        val uiEffect: Flow<LiveRouteTrackingUiEffect> = _uiEffect.receiveAsFlow()

        private var previousGoalReached = false
        private var hasShownLocationPermissionLostNotice = false

        init {
            viewModelScope.launch {
                activeSessionFlow.collect { session ->
                    when (session) {
                        is RunSession.Idle -> {
                            previousGoalReached = false
                            hasShownLocationPermissionLostNotice = false
                            showGoalCelebrationFlow.value = false
                        }

                        is RunSession.ActiveSession -> {
                            if (session.goalReached && !previousGoalReached) {
                                showGoalCelebrationFlow.value = true
                            }

                            previousGoalReached = session.goalReached
                        }
                    }
                }
            }

            viewModelScope.launch {
                @OptIn(ExperimentalCoroutinesApi::class)
                activeSessionFlow
                    .flatMapLatest { session ->
                        if (session is RunSession.ActiveSession && session.state == RunState.ACTIVE) {
                            locationRepository.getSignalQualityUpdates()
                        } else {
                            flowOf()
                        }
                    }.catch { error ->
                        if (error is SecurityException) {
                            onLocationPermissionRevoked()
                        }
                    }.collect()
            }
        }

        /** Posición sin recorrido registrando: con el recorrido activo, la posición sale de la traza. */
        @OptIn(ExperimentalCoroutinesApi::class)
        private val currentLocationFlow =
            activeSessionFlow.flatMapLatest { session ->
                val isTracking = session is RunSession.ActiveSession && session.state == RunState.ACTIVE
                if (isTracking) flowOf(null) else locationUpdatesOrNull()
            }

        /** Estado de los diálogos y avisos de finalizar, agrupado para que el `combine` de [uiState] quede tipado. */
        private val finishDialogsFlow =
            combine(
                showFinishConfirmDialogFlow,
                showShortRunDialogFlow,
                showGoalCelebrationFlow,
                finishFailedFlow,
                isFinishingFlow,
                ::FinishDialogs,
            )

        /** Lo que el mapa muestra además del recorrido: centrado, trazas guardadas y aviso sin sensor. */
        private val mapExtrasFlow =
            combine(
                isMapCenteredOnRunnerFlow,
                savedTracesFlow,
                regularityMonitor.state.map { it.sensorAvailable },
                ::MapExtras,
            )

        val uiState =
            combine(
                activeSessionFlow,
                isRunInProgressFlow,
                currentLocationFlow,
                finishDialogsFlow,
                mapExtrasFlow,
            ) { session, isRunInProgress, currentLocation, dialogs, extras ->
                when (session) {
                    is RunSession.Idle -> {
                        LiveRouteTrackingUiState(
                            isRunInProgress = isRunInProgress,
                            runnerPosition = currentLocation,
                            isMapCenteredOnRunner = extras.isCentered,
                            showFinishConfirmDialog = dialogs.showFinishConfirm,
                            showShortRunDialog = dialogs.showShortRun,
                            finishFailed = dialogs.finishFailed,
                            isFinishing = dialogs.isFinishing,
                            savedTraces = extras.savedTraces,
                        )
                    }

                    is RunSession.ActiveSession -> {
                        LiveRouteTrackingUiState(
                            hasActiveRun = true,
                            isRunInProgress = isRunInProgress,
                            runState = session.state,
                            goal = session.goal,
                            formattedTime = RunValueFormatter.formatDuration(session.metrics.elapsedTimeSeconds),
                            formattedDistance = RunValueFormatter.formatLiveDistanceKm(session.metrics.distanceMeters),
                            formattedSpeed = RunValueFormatter.formatSpeedValue(session.metrics.currentSpeedKmh),
                            formattedAverageSpeed = RunValueFormatter.formatSpeedValue(session.metrics.averageSpeedKmh),
                            routePoints = session.routePoints,
                            runnerPosition = session.routePoints.lastOrNull() ?: currentLocation,
                            isMapCenteredOnRunner = extras.isCentered,
                            gpsQuality = session.gpsQuality,
                            goalReached = session.goalReached,
                            showFinishConfirmDialog = dialogs.showFinishConfirm,
                            showShortRunDialog = dialogs.showShortRun,
                            showGoalCelebration = dialogs.showCelebration,
                            areControlActionsAllowed = true,
                            finishFailed = dialogs.finishFailed,
                            isFinishing = dialogs.isFinishing,
                            savedTraces = extras.savedTraces,
                            showMotionSensorMissing = !extras.sensorAvailable,
                        )
                    }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = LiveRouteTrackingUiState(),
            )

        fun onPauseClicked() {
            viewModelScope.launch {
                runSessionRepository.pauseSession()
            }
        }

        fun onResumeClicked() {
            viewModelScope.launch {
                runSessionRepository.resumeSession()
            }
        }

        fun onFinishRequested() {
            val session = activeSessionFlow.value as? RunSession.ActiveSession ?: return

            finishFailedFlow.value = false
            isFinishingFlow.value = false

            if (session.isShortRun) {
                showShortRunDialogFlow.value = true
            } else {
                showFinishConfirmDialogFlow.value = true
            }
        }

        /**
         * Finaliza el recorrido. El diálogo queda abierto mientras tanto: si sale bien se cierra y se
         * navega al resultado; si falla, la sesión sigue intacta y el diálogo ofrece reintentar.
         */
        fun onConfirmFinish() {
            if (isFinishingFlow.value) return

            isFinishingFlow.value = true
            finishFailedFlow.value = false

            viewModelScope.launch {
                finishRun()
                    .onSuccess { summary ->
                        isFinishingFlow.value = false
                        showFinishConfirmDialogFlow.value = false
                        _uiEffect.send(
                            LiveRouteTrackingUiEffect.NavigateToResults(summary),
                        )
                    }.onFailure {
                        isFinishingFlow.value = false
                        finishFailedFlow.value = true
                    }
            }
        }

        fun onCancelFinish() {
            isFinishingFlow.value = false
            finishFailedFlow.value = false
            showFinishConfirmDialogFlow.value = false
        }

        fun onDiscardShortRun() {
            showShortRunDialogFlow.value = false
            hasShownLocationPermissionLostNotice = false
            runSessionRepository.resetToIdle()
        }

        fun onReturnFromShortRun() {
            showShortRunDialogFlow.value = false
        }

        /** Vuelve a leer la ubicación (permiso y estado del dispositivo): decide si se dibujan las trazas. */
        fun onLocationStatusRefreshed() {
            locationStatusFlow.value = locationRepository.readLocationStatus()
        }

        /**
         * Toque sobre el mapa. Si pasa cerca de alguna traza dibujada, avisa que se abra ese recorrido (el más
         * reciente si coinciden varios); si no hay trazas dibujadas o el toque queda lejos, no hace nada.
         */
        fun onMapTapped(
            latitude: Double,
            longitude: Double,
            toleranceMeters: Double,
        ) {
            val run = displayedRunsFlow.value.mostRecentNear(latitude, longitude, toleranceMeters) ?: return
            _uiEffect.trySend(LiveRouteTrackingUiEffect.OpenSavedRun(run.id))
        }

        fun onCenterMapClicked() {
            isMapCenteredOnRunnerFlow.value = true
        }

        fun onMapDragged() {
            isMapCenteredOnRunnerFlow.value = false
        }

        fun onCelebrationDismissed() {
            showGoalCelebrationFlow.value = false
        }

        fun onLocationPermissionRevoked() {
            val session = runSessionRepository.activeSession.value
            val isActive = session is RunSession.ActiveSession

            if (isActive && !hasShownLocationPermissionLostNotice) {
                hasShownLocationPermissionLostNotice = true

                _uiEffect.trySend(LiveRouteTrackingUiEffect.ShowLocationPermissionLostMessage)
            }
        }

        private fun locationUpdatesOrNull(): Flow<RoutePoint?> =
            locationRepository
                .getLocationUpdates(LOCATION_INTERVAL_MILLIS)
                .map<RoutePoint, RoutePoint?> { it }
                .catch { emit(null) }
                .onStart { emit(null) }

        private companion object {
            const val LOCATION_INTERVAL_MILLIS = 1_000L
        }
    }

sealed interface LiveRouteTrackingUiEffect {
    data class NavigateToResults(
        val summary: FrozenRunSummary,
    ) : LiveRouteTrackingUiEffect

    /** Se tocó la traza de un recorrido guardado: abrir su detalle. */
    data class OpenSavedRun(
        val runId: String,
    ) : LiveRouteTrackingUiEffect

    /** Se perdió el permiso de ubicación con un recorrido en curso; el texto lo pone la pantalla. */
    data object ShowLocationPermissionLostMessage : LiveRouteTrackingUiEffect
}

private data class FinishDialogs(
    val showFinishConfirm: Boolean,
    val showShortRun: Boolean,
    val showCelebration: Boolean,
    val finishFailed: Boolean,
    val isFinishing: Boolean,
)

private data class MapExtras(
    val isCentered: Boolean,
    val savedTraces: List<SavedTraceUi>,
    val sensorAvailable: Boolean,
)
