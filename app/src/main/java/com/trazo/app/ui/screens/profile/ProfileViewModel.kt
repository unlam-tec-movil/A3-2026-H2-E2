package com.trazo.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.app.data.repository.SavedRunRepository
import com.trazo.app.domain.model.saved.RunBadge
import com.trazo.app.domain.model.saved.SavedRun
import com.trazo.app.domain.model.saved.SavedRunTotals
import com.trazo.app.ui.components.common.RunValueFormatter
import com.trazo.app.ui.components.common.StatusBadgeKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Datos de una card de «Mis recorridos». Fecha, distancia y duración ya vienen formateadas; el resto son datos
 * puros: las frases (la descripción accesible, «sin datos» de la regularidad) las arma el Composable.
 *
 * [badges] es una lista porque un recorrido puede ser a la vez incompleto y demo; queda vacía si no
 * tiene marcas. [regularityPercent] es `null` si no hay dato.
 */
data class RunCardUi(
    val id: String,
    val title: String,
    val dateText: String,
    val distanceText: String,
    val durationText: String,
    val brakeCount: Int,
    val regularityPercent: Int?,
    val badges: List<StatusBadgeKind>,
)

/**
 * Estado inmutable de la pestaña Perfil. Los totales no cuentan los recorridos demo.
 * [hasRecentRuns] se deriva de [runs]: no se guarda aparte.
 */
data class ProfileUiState(
    val runs: List<RunCardUi> = emptyList(),
    val totals: SavedRunTotals = SavedRunTotals(0, 0.0, 0),
) {
    val hasRecentRuns: Boolean get() = runs.isNotEmpty()
}

/**
 * Única fuente del estado de Perfil. Solo lee los recorridos guardados: muestra lo que
 * [SavedRunRepository.runs] emita, del más reciente al más antiguo. Que sobrevivan al cierre de la
 * app depende de que `save-run` reemplace el almacenamiento en memoria por Room (requisito 1.8).
 */
@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        savedRunRepository: SavedRunRepository,
    ) : ViewModel() {
        val uiState: StateFlow<ProfileUiState> =
            savedRunRepository.runs
                .map { runs -> runs.toUiState() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = ProfileUiState(),
                )

        private fun List<SavedRun>.toUiState(): ProfileUiState =
            ProfileUiState(
                runs = map { it.toCard() },
                totals = SavedRunTotals.from(this),
            )

        private fun SavedRun.toCard(): RunCardUi =
            RunCardUi(
                id = id,
                title = title,
                dateText = RunValueFormatter.formatShortDate(startedAt),
                distanceText = RunValueFormatter.formatDistanceKm(distanceMeters),
                durationText = RunValueFormatter.formatDuration(elapsedTimeSeconds),
                brakeCount = brakeCount,
                regularityPercent = regularityPercent,
                // La card no muestra «Meta cumplida»: solo lo que queda por mejorar o aclarar.
                badges =
                    badges()
                        .filterNot { it == RunBadge.GOAL_REACHED }
                        .map { it.toStatusBadgeKind() },
            )

        private fun RunBadge.toStatusBadgeKind(): StatusBadgeKind =
            when (this) {
                RunBadge.GOAL_REACHED -> StatusBadgeKind.GOAL_REACHED
                RunBadge.INCOMPLETE -> StatusBadgeKind.INCOMPLETE
                RunBadge.DEMO -> StatusBadgeKind.DEMO
            }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
