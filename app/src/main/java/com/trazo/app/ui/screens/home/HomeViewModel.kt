package com.trazo.app.ui.screens.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Estado inmutable de la pantalla de Inicio.
 *
 * Invariante: si [isStartRunEnabled] es `false`, [isStartRunOptionVisible] también es `false`.
 */
data class HomeUiState(
    val hasRecentRuns: Boolean = false,
    val isStartRunEnabled: Boolean = true,
    val isStartRunOptionVisible: Boolean = false,
)

/**
 * Única fuente del estado de Inicio. Procesa los eventos del FAB "+" y de la opción
 * "Iniciar un recorrido".
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        /** Muestra u oculta la opción. No hace nada si el FAB está deshabilitado. */
        fun onStartRunFabClick() {
            _uiState.update { state ->
                if (state.isStartRunEnabled) {
                    state.copy(isStartRunOptionVisible = !state.isStartRunOptionVisible)
                } else {
                    state
                }
            }
        }

        /** Cierra la opción sin iniciar un recorrido (toque fuera de la opción). */
        fun onStartRunOptionDismiss() {
            hideStartRunOption()
        }

        /** Cierra la opción al elegirla. La navegación la hace `HomeScreen`. */
        fun onStartRunOptionClick() {
            hideStartRunOption()
        }

        private fun hideStartRunOption() {
            _uiState.update { it.copy(isStartRunOptionVisible = false) }
        }
    }
