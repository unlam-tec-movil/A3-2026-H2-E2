package com.trazo.app.ui.screens.startrun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.app.data.clock.MonotonicClock
import com.trazo.app.data.repository.LocationRepository
import com.trazo.app.data.repository.RunSessionRepository
import com.trazo.app.domain.model.LocationStatus
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.usecases.AwaitPreciseLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Clock

/**
 * Estado inmutable del inicio de un recorrido: el botón «+», su opción, el modal, los avisos de
 * ubicación y la espera de señal GPS.
 *
 * Invariante: si [isFabEnabled] es `false`, [isOptionVisible] también es `false`.
 */
data class StartRunUiState(
    val isRunInProgress: Boolean = false,
    val isOptionVisible: Boolean = false,
    val isDialogVisible: Boolean = false,
    val selectedGoal: RunGoal? = null,
    val locationIssue: LocationStatus? = null,
    val waitingGoal: RunGoal? = null,
) {
    val isWaitingForGps: Boolean
        get() = waitingGoal != null

    val isFabEnabled: Boolean
        get() = !isRunInProgress && !isWaitingForGps

    val isConfirmEnabled: Boolean
        get() = selectedGoal != null
}

/** Acciones de un solo uso que necesitan la UI (permisos del sistema y navegación). */
sealed interface StartRunEffect {
    data object RequestLocationPermission : StartRunEffect

    data object NavigateToActivity : StartRunEffect
}

/**
 * Única fuente del flujo de inicio. Lo comparten Inicio y Mapa: `TrazoApp` lo crea fuera del
 * `NavHost` y les pasa el «+» ya armado.
 */
@HiltViewModel
class StartRunViewModel
    @Inject
    constructor(
        private val runSessionRepository: RunSessionRepository,
        private val locationRepository: LocationRepository,
        private val awaitPreciseLocation: AwaitPreciseLocationUseCase,
        private val monotonicClock: MonotonicClock,
        private val clock: Clock,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(StartRunUiState())
        val uiState: StateFlow<StartRunUiState> = _uiState.asStateFlow()

        private val effects = Channel<StartRunEffect>(Channel.BUFFERED)
        val uiEffect: Flow<StartRunEffect> = effects.receiveAsFlow()

        private var waitingJob: Job? = null

        init {
            viewModelScope.launch {
                runSessionRepository.isRunInProgress.collect { inProgress ->
                    _uiState.update { state ->
                        state.copy(
                            isRunInProgress = inProgress,
                            isOptionVisible = state.isOptionVisible && !inProgress,
                        )
                    }
                }
            }
        }

        /** Muestra u oculta la opción «Iniciar un recorrido». No hace nada si el «+» está deshabilitado. */
        fun onFabClick() {
            _uiState.update { state ->
                if (state.isFabEnabled) state.copy(isOptionVisible = !state.isOptionVisible) else state
            }
        }

        /** Oculta la opción sin iniciar nada (toque fuera de la opción). */
        fun onOptionDismiss() {
            _uiState.update { it.copy(isOptionVisible = false) }
        }

        /** Oculta la opción y revisa la ubicación antes de abrir el modal. */
        fun onOptionClick() {
            _uiState.update { it.copy(isOptionVisible = false) }
            val status = locationRepository.readLocationStatus()
            if (status == LocationStatus.PERMISSION_DENIED) {
                effects.trySend(StartRunEffect.RequestLocationPermission)
            } else {
                showDialogOrIssue(status)
            }
        }

        /** Vuelve a leer la ubicación después del diálogo de permisos del sistema. */
        fun onPermissionResult() {
            showDialogOrIssue(locationRepository.readLocationStatus())
        }

        /** Marca una meta. Si ya había otra, queda solo la nueva. */
        fun onGoalSelected(goal: RunGoal) {
            _uiState.update { it.copy(selectedGoal = goal) }
        }

        /** Cierra el modal sin comenzar nada y olvida la meta. */
        fun onDismiss() {
            _uiState.update { it.copy(isDialogVisible = false, selectedGoal = null) }
        }

        /** Cierra el aviso de ubicación sin hacer nada más. */
        fun onLocationIssueDismiss() {
            _uiState.update { it.copy(locationIssue = null) }
        }

        /**
         * Cierra el modal, lleva al Mapa y espera la señal GPS para comenzar con la meta elegida.
         * Sin meta no hace nada.
         */
        fun onConfirm() {
            val goal = _uiState.value.selectedGoal ?: return
            _uiState.update {
                it.copy(isDialogVisible = false, selectedGoal = null, waitingGoal = goal)
            }
            effects.trySend(StartRunEffect.NavigateToActivity)
            waitingJob?.cancel()
            waitingJob = viewModelScope.launch { awaitSignalAndStart(goal) }
        }

        /** Deja de esperar la señal sin comenzar ningún recorrido. */
        fun onCancelWaiting() {
            waitingJob?.cancel()
            waitingJob = null
            _uiState.update { it.copy(waitingGoal = null) }
        }

        private suspend fun awaitSignalAndStart(goal: RunGoal) {
            try {
                awaitPreciseLocation()
                runSessionRepository.attachActiveSession(
                    InitializedRunSession(
                        id = UUID.randomUUID().toString(),
                        goal = goal,
                        startMonotonicTimestamp = monotonicClock.nowMillis(),
                        startedAt = clock.now(),
                    ),
                )
                _uiState.update { it.copy(waitingGoal = null) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (
                @Suppress("TooGenericExceptionCaught") error: Exception,
            ) {
                val status = locationRepository.readLocationStatus()
                val issue = if (status == LocationStatus.READY) LocationStatus.DEVICE_LOCATION_OFF else status
                _uiState.update { it.copy(waitingGoal = null, locationIssue = issue) }
            }
        }

        private fun showDialogOrIssue(status: LocationStatus) {
            _uiState.update { state ->
                if (status == LocationStatus.READY) {
                    state.copy(isDialogVisible = true, selectedGoal = null, locationIssue = null)
                } else {
                    state.copy(isDialogVisible = false, locationIssue = status)
                }
            }
        }
    }
