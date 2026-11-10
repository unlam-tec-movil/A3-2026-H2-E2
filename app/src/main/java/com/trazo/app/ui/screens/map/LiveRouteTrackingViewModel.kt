package com.trazo.app.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RawLocationPoint
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.repository.LocationClient
import com.trazo.app.domain.usecases.AttachActiveRunSessionUseCase
import com.trazo.app.domain.usecases.FinishRunUseCase
import com.trazo.app.domain.usecases.GetActiveRunSessionUseCase
import com.trazo.app.domain.usecases.PauseRunUseCase
import com.trazo.app.domain.usecases.ResumeRunUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
        private val locationClient: LocationClient,
    ) : ViewModel() {
        private val activeSessionFlow = getActiveRunSessionUseCase()

        private val locationRefresh = MutableStateFlow(0)

        @OptIn(ExperimentalCoroutinesApi::class)
        private val currentLocationFlow =
            combine(activeSessionFlow, locationRefresh) { session, _ ->
                session
            }.flatMapLatest { session ->
                if (session?.state == RunState.ACTIVE) {
                    flowOf<RawLocationPoint?>(null)
                } else {
                    locationClient
                        .getLocationUpdates(1_000L)
                        .map<RawLocationPoint, RawLocationPoint?> { point ->
                            point
                        }.catch {
                            emit(null)
                        }
                }
            }

        val uiState =
            combine(activeSessionFlow, currentLocationFlow) {
                session,
                currentLocation,
                ->
                session.toUiState(currentLocation)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = LiveRouteTrackingUiState(),
            )

        private val _events = MutableSharedFlow<LiveRouteTrackingEvent>()
        val events = _events.asSharedFlow()

        fun refreshCurrentLocation() {
            locationRefresh.value++
        }

        fun attachActiveSession(session: InitializedRunSession) {
            attachActiveRunSessionUseCase(session)
        }

        fun pauseRun() {
            viewModelScope.launch {
                pauseRunUseCase().onFailure { exception ->
                    emitError(exception)
                }
            }
        }

        fun resumeRun() {
            viewModelScope.launch {
                resumeRunUseCase().onFailure { exception ->
                    emitError(exception)
                }
            }
        }

        fun finishRun() {
            viewModelScope.launch {
                finishRunUseCase()
                    .onSuccess { summary ->
                        _events.emit(
                            LiveRouteTrackingEvent.RunFinished(summary),
                        )
                    }.onFailure { exception ->
                        emitError(exception)
                    }
            }
        }

        private suspend fun emitError(exception: Throwable) {
            _events.emit(
                LiveRouteTrackingEvent.Error(
                    exception.message ?: "Ocurrió un error con el recorrido.",
                ),
            )
        }

        private fun RunSession?.toUiState(currentLocation: RawLocationPoint?): LiveRouteTrackingUiState {
            if (this == null) {
                return LiveRouteTrackingUiState(
                    currentLocation = currentLocation,
                )
            }

            val location =
                if (state == RunState.ACTIVE) {
                    routePoints.lastOrNull()?.toRawLocationPoint()
                } else {
                    currentLocation
                }

            return LiveRouteTrackingUiState(
                hasActiveRun = state != RunState.FINISHED,
                runState = state,
                goal = goal,
                routePoints = routePoints,
                elapsedTimeSeconds = metrics.elapsedTimeSeconds,
                distanceMeters = metrics.distanceMeters,
                currentSpeedKmh = metrics.currentSpeedKmh,
                averageSpeedKmh = metrics.averageSpeedKmh,
                gpsQuality = gpsQuality,
                currentLocation = location,
            )
        }

        private fun RoutePoint.toRawLocationPoint() =
            RawLocationPoint(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = accuracyMeters,
                speedMetersPerSecond = speedMetersPerSecond,
                monotonicTimestampMillis = timestampMillis,
            )
    }

sealed interface LiveRouteTrackingEvent {
    data class RunFinished(
        val summary: FrozenRunSummary,
    ) : LiveRouteTrackingEvent

    data class Error(
        val message: String,
    ) : LiveRouteTrackingEvent
}
