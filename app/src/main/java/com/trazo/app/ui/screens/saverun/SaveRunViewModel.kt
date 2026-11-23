package com.trazo.app.ui.screens.saverun

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.app.domain.model.saved.SavedRun
import com.trazo.app.domain.usecases.SaveRunResult
import com.trazo.app.domain.usecases.SaveRunUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SaveRunUiState(
    val title: String = "",
    val highlights: Set<String> = emptySet(),
    val photoPaths: List<String> = emptyList(),
    /** Verdadero si el último intento de guardar falló por un título inválido. */
    val titleError: Boolean = false,
)

sealed interface SaveRunEffect {
    /** El recorrido se guardó: la pantalla vuelve al mapa. */
    data object Saved : SaveRunEffect

    /** El recorrido ya no está disponible: no hay nada que guardar. */
    data object Close : SaveRunEffect
}

/** Guarda lo que escribe el usuario y, al tocar «Guardar», delega las reglas en [SaveRunUseCase]. */
@HiltViewModel
class SaveRunViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val saveRun: SaveRunUseCase,
    ) : ViewModel() {
        private val runId: String = savedStateHandle.get<String>(RUN_ID_KEY).orEmpty()

        private val _uiState = MutableStateFlow(SaveRunUiState())
        val uiState: StateFlow<SaveRunUiState> = _uiState.asStateFlow()

        private val _effect = Channel<SaveRunEffect>(Channel.BUFFERED)
        val effect: Flow<SaveRunEffect> = _effect.receiveAsFlow()

        fun onTitleChange(title: String) {
            _uiState.update { it.copy(title = title.take(SavedRun.TITLE_MAX_LENGTH), titleError = false) }
        }

        /** Marca o desmarca un destacado. */
        fun onHighlightToggle(highlight: String) {
            _uiState.update { state ->
                val updated =
                    if (highlight in state.highlights) state.highlights - highlight else state.highlights + highlight
                state.copy(highlights = updated)
            }
        }

        fun onSave() {
            viewModelScope.launch {
                val state = _uiState.value
                when (saveRun(runId, state.title, state.highlights.toList(), state.photoPaths)) {
                    SaveRunResult.Saved -> _effect.send(SaveRunEffect.Saved)
                    SaveRunResult.InvalidTitle -> _uiState.update { it.copy(titleError = true) }
                    SaveRunResult.RunNotFound -> _effect.send(SaveRunEffect.Close)
                    // La pantalla no deja agregar más fotos que el máximo, así que no debería pasar.
                    SaveRunResult.TooManyPhotos -> Unit
                }
            }
        }

        private companion object {
            const val RUN_ID_KEY = "runId"
        }
    }
