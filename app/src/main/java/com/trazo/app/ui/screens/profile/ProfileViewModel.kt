package com.trazo.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Estado inmutable de la pestaña Perfil. Los totales quedan en cero hasta que existan recorridos
 * guardados (save-run): el ViewModel todavía no calcula nada.
 */
data class ProfileUiState(
    val hasRecentRuns: Boolean = false,
    val totalRuns: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val totalTimeSeconds: Long = 0,
)

/** Única fuente del estado de Perfil. */
@HiltViewModel
class ProfileViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(ProfileUiState())
        val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    }
