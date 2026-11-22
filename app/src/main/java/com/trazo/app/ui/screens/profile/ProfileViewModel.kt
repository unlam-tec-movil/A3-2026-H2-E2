package com.trazo.app.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.app.R
import com.trazo.app.data.repository.SavedRunRepository
import com.trazo.app.domain.model.saved.RunBadge
import com.trazo.app.domain.model.saved.SavedRun
import com.trazo.app.domain.model.saved.SavedRunTotals
import com.trazo.app.ui.components.common.RunValueFormatter
import com.trazo.app.ui.components.common.StatusBadgeKind
import com.trazo.app.ui.components.common.toStatusBadgeKind
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Datos de una card de «Mis recorridos», ya formateados.
 *
 * [badges] es una lista porque un recorrido puede ser a la vez incompleto y demo; queda vacía si no
 * tiene marcas. [accessibilityLabel] reúne todos los datos y las marcas en una sola frase.
 */
data class RunCardUi(
    val id: String,
    val title: String,
    val dateText: String,
    val distanceText: String,
    val durationText: String,
    val brakeCount: Int,
    val regularityText: String,
    val badges: List<StatusBadgeKind>,
    val accessibilityLabel: String,
)

/**
 * Estado inmutable de la pestaña Perfil. Los totales no cuentan los recorridos demo.
 * [hasRecentRuns] se deriva de [runs]: no se guarda aparte.
 */
data class ProfileUiState(
    val runs: List<RunCardUi> = emptyList(),
    val totalRuns: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val totalTimeSeconds: Long = 0,
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
        @param:ApplicationContext private val context: Context,
    ) : ViewModel() {
        val uiState: StateFlow<ProfileUiState> =
            savedRunRepository.runs
                .map { runs -> runs.toUiState() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = ProfileUiState(),
                )

        private fun List<SavedRun>.toUiState(): ProfileUiState {
            val totals = SavedRunTotals.from(this)
            return ProfileUiState(
                runs = map { it.toCard() },
                totalRuns = totals.runCount,
                totalDistanceKm = totals.distanceKm,
                totalTimeSeconds = totals.timeSeconds,
            )
        }

        private fun SavedRun.toCard(): RunCardUi {
            val dateText = RunValueFormatter.formatShortDate(startedAt)
            val distanceText = RunValueFormatter.formatDistanceKm(distanceMeters)
            val durationText = RunValueFormatter.formatDuration(elapsedTimeSeconds)
            val regularityText = RunValueFormatter.formatRegularity(regularityPercent)
            // La card no muestra «Meta cumplida»: solo lo que queda por mejorar o aclarar.
            val badges =
                badges()
                    .filterNot { it == RunBadge.GOAL_REACHED }
                    .map { it.toStatusBadgeKind() }
            return RunCardUi(
                id = id,
                title = title,
                dateText = dateText,
                distanceText = distanceText,
                durationText = durationText,
                brakeCount = brakeCount,
                regularityText = regularityText,
                badges = badges,
                accessibilityLabel =
                    accessibilityLabel(
                        title = title,
                        dateText = dateText,
                        distanceText = distanceText,
                        durationText = durationText,
                        brakeCount = brakeCount,
                        regularityDescription =
                            regularityPercent?.let { regularityText } ?: context.getString(
                                R.string.profile_run_regularity_none_description,
                            ),
                        badges = badges,
                    ),
            )
        }

        private fun accessibilityLabel(
            title: String,
            dateText: String,
            distanceText: String,
            durationText: String,
            brakeCount: Int,
            regularityDescription: String,
            badges: List<StatusBadgeKind>,
        ): String {
            val resources = context.resources
            val base =
                context.getString(
                    R.string.profile_run_card_description,
                    title,
                    dateText,
                    distanceText,
                    durationText,
                    resources.getQuantityString(R.plurals.profile_run_brakes_description, brakeCount, brakeCount),
                    regularityDescription,
                )
            if (badges.isEmpty()) return base
            val badgeNames = badges.joinToString(", ") { context.getString(it.labelRes()) }
            return base + " " + context.getString(R.string.profile_run_card_badges_description, badgeNames)
        }

        private fun StatusBadgeKind.labelRes(): Int =
            when (this) {
                StatusBadgeKind.GOAL_REACHED -> R.string.profile_run_badge_goal_reached
                StatusBadgeKind.INCOMPLETE -> R.string.profile_run_badge_incomplete
                StatusBadgeKind.DEMO -> R.string.profile_run_badge_demo
            }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
