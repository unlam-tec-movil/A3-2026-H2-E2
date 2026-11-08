package com.trazo.app.ui.screens.startrun

import androidx.lifecycle.ViewModel
import com.trazo.app.domain.model.RunGoal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Estado inmutable del modal "Iniciar nuevo recorrido".
 *
 * "Comenzar" solo se habilita cuando hay una meta elegida.
 */
data class StartRunUiState(
    val isDialogVisible: Boolean = false,
    val selectedGoal: RunGoal? = null,
) {
    val isConfirmEnabled: Boolean
        get() = selectedGoal != null
}

/**
 * Única fuente del estado del modal. Lo usan Inicio y Mapa.
 */
@HiltViewModel
class StartRunViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(StartRunUiState())
        val uiState: StateFlow<StartRunUiState> = _uiState.asStateFlow()

        /** Abre el modal sin ninguna meta elegida. */
        fun onStartRunRequested() {
            _uiState.value = StartRunUiState(isDialogVisible = true)
        }

        /** Marca una meta. Si ya había otra, queda solo la nueva. */
        fun onGoalSelected(goal: RunGoal) {
            _uiState.update { it.copy(selectedGoal = goal) }
        }

        /** Cierra el modal sin comenzar nada y olvida la meta. */
        fun onDismiss() {
            _uiState.value = StartRunUiState()
        }

        /**
         * Cierra el modal y devuelve la meta elegida para comenzar el recorrido.
         * Si no hay meta, no hace nada y devuelve `null`.
         */
        fun onConfirm(): RunGoal? {
            val goal = _uiState.value.selectedGoal ?: return null
            _uiState.value = StartRunUiState()
            return goal
        }
    }
