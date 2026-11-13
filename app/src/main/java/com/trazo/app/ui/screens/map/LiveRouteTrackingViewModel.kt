package com.trazo.app.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RawLocationPoint
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.repository.LocationClient
import com.trazo.app.domain.repository.RunSessionRepository
import com.trazo.app.domain.usecases.AttachActiveRunSessionUseCase
import com.trazo.app.domain.usecases.FinishRunUseCase
import com.trazo.app.domain.usecases.GetActiveRunSessionUseCase
import com.trazo.app.domain.usecases.PauseRunUseCase
import com.trazo.app.domain.usecases.ResumeRunUseCase
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
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LiveRouteTrackingViewModel
    @Inject
    constructor(
        getActiveRunSessionUseCase: GetActiveRunSessionUseCase,
        private val attachActiveRunSessionUseCase: AttachActiveRunSessionUseCase,
        private val pauseRunUseCase: PauseRunUseCase,
        private val resumeRunUseCase: ResumeRunUseCase,
        private val finishRunUseCase: FinishRunUseCase,
        private val runSessionRepository: RunSessionRepository,
        private val locationClient: LocationClient,
    ) : ViewModel() {
        private val activeSessionFlow = getActiveRunSessionUseCase()

//        private val hasActiveSession = MutableStateFlow(false)
        private val isRunInProgressFlow = runSessionRepository.isRunInProgress

        private val showFinishConfirmDialogFlow = MutableStateFlow(false)
        private val showShortRunDialogFlow = MutableStateFlow(false)
        private val showGoalCelebrationFlow = MutableStateFlow(false)
        private val isMapCenteredOnRunnerFlow = MutableStateFlow(true)

        private var previousGoalReached = false
        private var hasShownLocationPermissionLostNotice = false

        init {
            viewModelScope.launch {
                activeSessionFlow.collect { session ->
                    when (session) {
                        is RunSession.Idle -> {
//                            hasActiveSession.value = false
                            previousGoalReached = false
                            hasShownLocationPermissionLostNotice = false
                            showGoalCelebrationFlow.value = false
                        }

                        is RunSession.ActiveSession -> {
//                            hasActiveSession.value = true

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
                            locationClient.getSignalQualityUpdates()
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

        @OptIn(ExperimentalCoroutinesApi::class)
        private val currentLocationFlow =
            activeSessionFlow.flatMapLatest { session ->
                when (session) {
                    is RunSession.ActiveSession -> {
                        if (session.state == RunState.ACTIVE) {
                            flowOf<RoutePoint?>(null)
                        } else {
                            locationClient
                                .getLocationUpdates(1_000L)
                                .map<RawLocationPoint, RoutePoint?> { point ->
                                    RoutePoint(
                                        latitude = point.latitude,
                                        longitude = point.longitude,
                                        accuracyMeters = point.accuracyMeters,
                                        speedMetersPerSecond = point.speedMetersPerSecond,
                                        monotonicTimestampMillis = point.monotonicTimestampMillis,
                                    )
                                }.catch { emit(null) }
                                .onStart { emit(null) }
                        }
                    }

                    is RunSession.Idle -> {
                        locationClient
                            .getLocationUpdates(1_000L)
                            .map<RawLocationPoint, RoutePoint?> { point ->
                                RoutePoint(
                                    latitude = point.latitude,
                                    longitude = point.longitude,
                                    accuracyMeters = point.accuracyMeters,
                                    speedMetersPerSecond = point.speedMetersPerSecond,
                                    monotonicTimestampMillis = point.monotonicTimestampMillis,
                                )
                            }.catch { emit(null) }
                            .onStart { emit(null) }
                    }
                }
            }

        private val _uiEffect = Channel<LiveRouteTrackingUiEffect>(Channel.BUFFERED)

        val uiEffect: Flow<LiveRouteTrackingUiEffect> = _uiEffect.receiveAsFlow()

        val uiState =
            combine(
                activeSessionFlow,
                isRunInProgressFlow,
                currentLocationFlow,
                showFinishConfirmDialogFlow,
                showShortRunDialogFlow,
                showGoalCelebrationFlow,
                isMapCenteredOnRunnerFlow,
            ) { args: Array<Any?> ->
                val session = args[0] as RunSession
                val isRunInProgress = args[1] as Boolean
                val currentLocation = args[2] as? RoutePoint
                val showFinish = args[3] as Boolean
                val showShort = args[4] as Boolean
                val showCelebration = args[5] as Boolean
                val isCentered = args[6] as Boolean

                when (session) {
                    is RunSession.Idle -> {
                        LiveRouteTrackingUiState(
                            hasActiveRun = false,
                            isRunInProgress = isRunInProgress,
                            runState = RunState.IDLE,
                            goal = null,
                            formattedTime = "00:00:00",
                            formattedDistance = "0",
                            formattedSpeed = "0",
                            formattedAverageSpeed = "0",
                            routePoints = emptyList(),
                            runnerPosition = currentLocation,
                            isMapCenteredOnRunner = isCentered,
                            gpsQuality = GpsQuality.NO_SIGNAL,
                            goalReached = false,
                            showFinishConfirmDialog = showFinish,
                            showShortRunDialog = showShort,
                            showGoalCelebration = false,
                            areControlActionsAllowed = false,
                            rawDistanceMeters = 0.0,
                        )
                    }

                    is RunSession.ActiveSession -> {
                        val lastPoint = session.routePoints.lastOrNull()
                        val runnerPos = lastPoint ?: currentLocation

                        LiveRouteTrackingUiState(
                            hasActiveRun = true,
                            isRunInProgress = isRunInProgress,
                            runState = session.state,
                            goal = session.goal,
                            formattedTime = formatElapsedTime(session.metrics.elapsedTimeSeconds),
                            formattedDistance = formatDistance(session.metrics.distanceMeters),
                            formattedSpeed = formatSpeed(session.metrics.currentSpeedKmh),
                            formattedAverageSpeed = formatSpeed(session.metrics.averageSpeedKmh),
                            routePoints = session.routePoints,
                            runnerPosition = runnerPos,
                            isMapCenteredOnRunner = isCentered,
                            gpsQuality = session.gpsQuality,
                            goalReached = session.goalReached,
                            showFinishConfirmDialog = showFinish,
                            showShortRunDialog = showShort,
                            showGoalCelebration = showCelebration,
                            areControlActionsAllowed = true,
                            rawDistanceMeters = session.metrics.distanceMeters,
                        )
                    }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = LiveRouteTrackingUiState(),
            )

        fun attachActiveSession(session: InitializedRunSession) {
            attachActiveRunSessionUseCase(session)
        }

        fun onPauseClicked() {
            viewModelScope.launch {
                pauseRunUseCase()
            }
        }

        fun onResumeClicked() {
            viewModelScope.launch {
                resumeRunUseCase()
            }
        }

        fun onFinishRequested() {
            if (!uiState.value.hasActiveRun) return

            val currentState = uiState.value

            if (currentState.rawDistanceMeters < 200.0) {
                showShortRunDialogFlow.value = true
            } else {
                showFinishConfirmDialogFlow.value = true
            }
        }

        fun onConfirmFinish() {
            showFinishConfirmDialogFlow.value = false

            viewModelScope.launch {
                finishRunUseCase().onSuccess { summary ->
                    _uiEffect.send(
                        LiveRouteTrackingUiEffect.NavigateToResults(summary),
                    )
                }
            }
        }

        fun onCancelFinish() {
            showFinishConfirmDialogFlow.value = false
        }

        fun onDiscardShortRun() {
            showShortRunDialogFlow.value = false
            hasShownLocationPermissionLostNotice = false
            runSessionRepository.resetToIdle()

            viewModelScope.launch {
                _uiEffect.send(LiveRouteTrackingUiEffect.ReturnToIdleMap)
            }
        }

        fun onReturnFromShortRun() {
            showShortRunDialogFlow.value = false
        }

        fun onStartRunClicked() {
            viewModelScope.launch {
                _uiEffect.send(LiveRouteTrackingUiEffect.OpenStartRunModal)
            }
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

                _uiEffect.trySend(
                    LiveRouteTrackingUiEffect.ShowLocationPermissionLostMessage(
                        LOCATION_PERMISSION_LOST_MESSAGE,
                    ),
                )
            }
        }

        private companion object {
            const val LOCATION_PERMISSION_LOST_MESSAGE =
                "Se perdió el permiso de ubicación. Activalo desde Ajustes para seguir registrando tu recorrido."

            val SPANISH_LOCALE: Locale = Locale.forLanguageTag("es-AR")

            fun formatElapsedTime(seconds: Long): String {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                val remainingSeconds = seconds % 60

                return String.format(
                    Locale.ROOT,
                    "%02d:%02d:%02d",
                    hours,
                    minutes,
                    remainingSeconds,
                )
            }

            fun formatDistance(meters: Double): String =
                String.format(
                    SPANISH_LOCALE,
                    "%.2f",
                    meters / 1000.0,
                )

            fun formatSpeed(speedKmh: Double): String =
                String.format(
                    SPANISH_LOCALE,
                    "%.1f",
                    speedKmh,
                )
        }
    }

sealed interface LiveRouteTrackingUiEffect {
    data class NavigateToResults(
        val summary: FrozenRunSummary,
    ) : LiveRouteTrackingUiEffect

    data object OpenStartRunModal : LiveRouteTrackingUiEffect

    data object ReturnToIdleMap : LiveRouteTrackingUiEffect

    data class ShowLocationPermissionLostMessage(
        val message: String,
    ) : LiveRouteTrackingUiEffect
}
