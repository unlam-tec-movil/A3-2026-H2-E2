package com.trazo.app.ui.screens.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Estado inmutable de la pantalla de Inicio. El botón «+» y su opción ya no viven acá: los maneja
 * `StartRunViewModel`, compartido con el Mapa.
 */
data class HomeUiState(
    val hasRecentRuns: Boolean = false,
)

/** Única fuente del estado de Inicio. */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    }
