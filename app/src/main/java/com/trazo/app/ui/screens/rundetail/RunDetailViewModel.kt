package com.trazo.app.ui.screens.rundetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.app.data.repository.SavedRunRepository
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.saved.RunBadge
import com.trazo.app.domain.model.saved.SavedRun
import com.trazo.app.ui.components.common.RunValueFormatter
import com.trazo.app.ui.components.common.StatusBadgeKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Detalle de un recorrido guardado, ya formateado para el modal.
 *
 * Las secciones de destacados y de fotos se ocultan con listas vacías. [photoPaths] trae las fotos del recorrido.
 * [routePoints] con menos de 2 puntos no se puede dibujar: el mapa muestra un texto alternativo. Lleva datos y no
 * frases: la meta, la distancia y la duración se arman en el Composable con los string resources.
 */
data class RunDetailUi(
    val id: String,
    val title: String,
    val averageSpeedText: String,
    val goal: RunGoal,
    val badges: List<StatusBadgeKind>,
    val highlights: List<String>,
    val photoPaths: List<String>,
    val routePoints: List<RoutePoint>,
    val distanceText: String,
    val durationText: String,
)

/**
 * Recorrido abierto en el modal de detalle. Lo crea `TrazoApp`, que es el único dueño del modal:
 * Perfil y Actividad solo piden [open] con un id. Solo lee los recorridos guardados.
 */
@HiltViewModel
class RunDetailViewModel
    @Inject
    constructor(
        private val savedRunRepository: SavedRunRepository,
    ) : ViewModel() {
        private val openRun = MutableStateFlow<SavedRun?>(null)
        private var openJob: Job? = null

        /** El recorrido abierto, o `null` si no hay ninguno. */
        val detail: StateFlow<RunDetailUi?> =
            openRun
                .map { it?.toUi() }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

        /** Abre el detalle de [runId]. Si ese recorrido no existe, no abre nada. */
        fun open(runId: String) {
            openJob?.cancel()
            openJob = viewModelScope.launch { openRun.value = savedRunRepository.getById(runId) }
        }

        fun close() {
            openJob?.cancel()
            openRun.value = null
        }

        private fun SavedRun.toUi(): RunDetailUi =
            RunDetailUi(
                id = id,
                title = title,
                averageSpeedText = RunValueFormatter.formatAverageSpeedKmh(averageSpeedKmh),
                goal = goal,
                badges = badges().map { it.toStatusBadgeKind() },
                highlights = highlights,
                photoPaths = photoPaths,
                routePoints = routePoints,
                distanceText = RunValueFormatter.formatDistanceKm(distanceMeters),
                durationText = RunValueFormatter.formatDuration(elapsedTimeSeconds),
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
