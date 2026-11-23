package com.trazo.app.ui.screens.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.trazo.app.data.repository.RunSessionRepository
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.ui.components.common.RunValueFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

data class ResultUiState(
    val isLoaded: Boolean = false,
    val isGoalReached: Boolean = false,
    val goal: RunGoal? = null,
    val distanceText: String = "",
    val timeText: String = "",
    /** `null` = la pantalla muestra «sin dato». */
    val regularityText: String? = null,
)

sealed interface ResultEffect {
    data object LeaveToMap : ResultEffect
}

/** Arma el estado del resultado a partir del resumen congelado, sin recalcular métricas ni mirar el modo demo. */
@HiltViewModel
class ResultViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        runSessionRepository: RunSessionRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ResultUiState())
        val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

        private val _effect = Channel<ResultEffect>(Channel.BUFFERED)
        val effect: Flow<ResultEffect> = _effect.receiveAsFlow()

        /** Id del recorrido que se muestra; lo recibe «Continuar» para pasar a «Guardar recorrido». */
        val runId: String = savedStateHandle.get<String>(RUN_ID_KEY).orEmpty()

        init {
            val summary = runSessionRepository.getFinishedSummary(runId)
            if (summary == null) {
                _effect.trySend(ResultEffect.LeaveToMap)
            } else {
                _uiState.value =
                    ResultUiState(
                        isLoaded = true,
                        isGoalReached = summary.goalReached,
                        goal = summary.goal,
                        distanceText = RunValueFormatter.formatDistanceKm(summary.metrics.distanceMeters),
                        timeText = RunValueFormatter.formatDuration(summary.metrics.elapsedTimeSeconds),
                        regularityText = summary.regularityPercent?.let(RunValueFormatter::formatRegularity),
                    )
            }
        }

        private companion object {
            const val RUN_ID_KEY = "runId"
        }
    }
