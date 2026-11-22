package com.trazo.app.ui.screens.rundetail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.app.R
import com.trazo.app.data.repository.SavedRunRepository
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.saved.SavedRun
import com.trazo.app.ui.components.common.RunValueFormatter
import com.trazo.app.ui.components.common.StatusBadgeKind
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Detalle de un recorrido guardado, ya formateado para el modal.
 *
 * Las secciones de destacados y de fotos se ocultan con listas vacías. [photoPaths] trae las fotos del recorrido.
 * [routePoints] con menos de 2 puntos no se puede dibujar: el mapa muestra un texto alternativo.
 */
data class RunDetailUi(
    val id: String,
    val title: String,
    val averageSpeedText: String,
    val goalText: String,
    val statusBadge: StatusBadgeKind,
    val isDemo: Boolean,
    val highlights: List<String>,
    val photoPaths: List<String>,
    val routePoints: List<RoutePoint>,
    val mapDescription: String,
)

/**
 * Recorrido abierto en el modal de detalle. Lo crea `TrazoApp`, que es el único dueño del modal:
 * Perfil y Actividad solo piden [open] con un id. Solo lee los recorridos guardados.
 */
@HiltViewModel
class RunDetailViewModel
    @Inject
    constructor(
        savedRunRepository: SavedRunRepository,
        @param:ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val runs: StateFlow<List<SavedRun>> =
            savedRunRepository.runs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        private val openRunId = MutableStateFlow<String?>(null)

        /** El recorrido abierto, o `null` si no hay ninguno. Pasa a `null` si el recorrido desaparece. */
        val detail: StateFlow<RunDetailUi?> =
            combine(openRunId, runs) { id, runs -> runs.firstOrNull { it.id == id }?.toUi() }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

        /** Abre el detalle de [runId]. Si ese recorrido no existe, no abre nada. */
        fun open(runId: String) {
            openRunId.value = runId.takeIf { id -> runs.value.any { it.id == id } }
        }

        fun close() {
            openRunId.value = null
        }

        private fun SavedRun.toUi(): RunDetailUi =
            RunDetailUi(
                id = id,
                title = title,
                averageSpeedText = RunValueFormatter.formatAverageSpeedKmh(averageSpeedKmh),
                goalText = context.getString(R.string.run_detail_goal_label, context.getString(goal.labelRes())),
                statusBadge = if (goalReached) StatusBadgeKind.GOAL_REACHED else StatusBadgeKind.INCOMPLETE,
                isDemo = isDemo,
                highlights = highlights,
                photoPaths = photoPaths,
                routePoints = routePoints,
                mapDescription =
                    context.getString(
                        R.string.run_detail_map_description,
                        title,
                        RunValueFormatter.formatDistanceKm(distanceMeters),
                        RunValueFormatter.formatDuration(elapsedTimeSeconds),
                    ),
            )

        private fun RunGoal.labelRes(): Int =
            when (this) {
                RunGoal.THREE_KILOMETERS -> R.string.map_goal_3km
                RunGoal.FIVE_KILOMETERS -> R.string.map_goal_5km
                RunGoal.TEN_KILOMETERS -> R.string.map_goal_10km
            }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
