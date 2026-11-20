package com.trazo.app.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.app.domain.model.stops.QueryStatus
import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopKind
import com.trazo.app.domain.model.stops.StopPhase
import com.trazo.app.domain.services.StopMonitor
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
        private val stopMonitor: StopMonitor,
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
                combine(stopMonitor.state, suppressed) { state, isSuppressed -> state to isSuppressed }
                    .collect { (state, isSuppressed) -> process(state, isSuppressed) }
            }
        }

        fun onClassify(kind: StopKind) {
            stopMonitor.answer(kind)
        }

        fun onSuppressedChanged(isSuppressed: Boolean) {
            suppressed.value = isSuppressed
        }

        private fun process(
            state: StopDetectionState,
            isSuppressed: Boolean,
        ) {
            val seen = seenBrakeIds
            val braking = state.phase as? StopPhase.Braking
            val newBrakeId =
                braking
                    ?.takeIf { seen != null && it.brake.id !in seen && it.query != QueryStatus.PENDING }
                    ?.brake
                    ?.id
            seenBrakeIds = (seen ?: emptySet()) + listOfNotNull(braking?.brake?.id)

            val pendingId = state.pendingClassificationBrakeId
            when {
                isSuppressed -> hideAlert()
                newBrakeId != null -> showAlert(newBrakeId)
                else ->
                    _uiState.value.alert?.let { alert ->
                        // La pausa personal descarta la frenada: su alerta ya no corresponde.
                        if (state.phase is StopPhase.PersonalPause || pendingId != null) hideAlert()
                    }
            }

            _uiState.update { it.copy(classificationVisible = !isSuppressed && pendingId != null) }

            if (isSuppressed && pendingId != null) {
                stopMonitor.answer(StopKind.BRAKE)
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
