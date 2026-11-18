package com.trazo.app.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.app.domain.model.stops.QueryStatus
import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopKind
import com.trazo.app.domain.model.stops.StopPhase
import com.trazo.app.domain.repository.StopDetectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Alerta de frenada visible: identifica la frenada que la originó. */
data class BrakeAlertUi(
    val brakeId: String,
)

/**
 * Estado de interfaz de la detección de frenadas.
 *
 * Invariante: con `suppressed` verdadero, [alert] es `null` y [classificationVisible] es `false`.
 */
data class StopDetectionUiState(
    val alert: BrakeAlertUi? = null,
    val classificationVisible: Boolean = false,
)

/**
 * Traduce el estado de detección en alerta (cierre a los 4 s) y diálogo de consulta.
 *
 * Con `suppressed` (cierre del recorrido, HU-06) no muestra nada, pero la frenada sigue registrada y
 * una consulta pendiente se resuelve como frenada.
 */
@HiltViewModel
class StopDetectionViewModel
    @Inject
    constructor(
        private val repository: StopDetectionRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(StopDetectionUiState())
        val uiState: StateFlow<StopDetectionUiState> = _uiState.asStateFlow()

        private val suppressed = MutableStateFlow(false)

        // Frenadas ya vistas: evita alertas de frenadas viejas o ocurridas durante la supresión.
        // `null` hasta la primera emisión, cuyas frenadas se consideran previas.
        private var seenBrakeIds: Set<String>? = null
        private var alertJob: Job? = null

        init {
            viewModelScope.launch {
                combine(repository.state, suppressed) { state, isSuppressed -> state to isSuppressed }
                    .collect { (state, isSuppressed) -> process(state, isSuppressed) }
            }
        }

        fun onClassify(kind: StopKind) {
            repository.classifyCurrentStop(kind)
        }

        fun onSuppressedChanged(isSuppressed: Boolean) {
            suppressed.value = isSuppressed
        }

        private fun process(
            state: StopDetectionState,
            isSuppressed: Boolean,
        ) {
            val seen = seenBrakeIds
            val currentIds = state.brakes.mapTo(mutableSetOf()) { it.id }
            val newBrakeId =
                (state.phase as? StopPhase.Braking)
                    ?.takeIf { seen != null && it.brakeId !in seen && it.brakeId in currentIds }
                    ?.takeIf { it.query != QueryStatus.PENDING }
                    ?.brakeId
            seenBrakeIds = (seen ?: emptySet()) + currentIds

            val pendingId = state.pendingClassificationBrakeId
            when {
                isSuppressed -> hideAlert()
                newBrakeId != null -> showAlert(newBrakeId)
                else ->
                    _uiState.value.alert?.let { alert ->
                        if (alert.brakeId !in currentIds || pendingId != null) hideAlert()
                    }
            }

            _uiState.update { it.copy(classificationVisible = !isSuppressed && pendingId != null) }

            if (isSuppressed && pendingId != null) {
                repository.classifyCurrentStop(StopKind.BRAKE)
            }
        }

        private fun showAlert(brakeId: String) {
            alertJob?.cancel()
            _uiState.update { it.copy(alert = BrakeAlertUi(brakeId)) }
            alertJob =
                viewModelScope.launch {
                    delay(ALERT_DURATION_MILLIS)
                    _uiState.update { state ->
                        if (state.alert?.brakeId == brakeId) state.copy(alert = null) else state
                    }
                }
        }

        private fun hideAlert() {
            alertJob?.cancel()
            alertJob = null
            _uiState.update { it.copy(alert = null) }
        }

        companion object {
            const val ALERT_DURATION_MILLIS = 4_000L
        }
    }
