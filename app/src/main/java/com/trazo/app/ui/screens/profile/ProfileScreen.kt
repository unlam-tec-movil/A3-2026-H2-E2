package com.trazo.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.app.R
import com.trazo.app.domain.model.saved.SavedRunTotals
import com.trazo.app.ui.components.common.RunValueFormatter
import com.trazo.app.ui.components.common.StatusBadgeKind
import com.trazo.app.ui.screens.profile.components.EmptyState
import com.trazo.app.ui.screens.profile.components.ProfileSectionCard
import com.trazo.app.ui.screens.profile.components.ProfileStatColumn
import com.trazo.app.ui.screens.profile.components.RunCard
import com.trazo.app.ui.screens.profile.components.TrazoTopBar
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

/**
 * Conecta [ProfileViewModel] con [ProfileContent]. No dibuja nada propio.
 *
 * @param onSettingsClick se invoca al tocar el botón de Configuración de la barra superior.
 * @param onRunClick se invoca con el id del recorrido cuya card se tocó. Esta pantalla no abre
 * nada: quien la usa decide qué hacer (el detalle lo abre `TrazoApp`).
 */
@Composable
fun ProfileScreen(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRunClick: (String) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileContent(
        state = state,
        onSettingsClick = onSettingsClick,
        onRunClick = onRunClick,
        modifier = modifier,
    )
}

/**
 * Contenido de Perfil. Se dibuja solo a partir de [state]; no guarda estado propio.
 * Arriba va la barra superior con "Trazo" y Configuración; debajo, en una lista perezosa, la card
 * «Mis estadísticas» (tres barras animadas con tope, valor y etiqueta) y «Mis recorridos». Sin
 * recorridos, «Mis recorridos» es una card con el estado vacío y los totales quedan en cero; con
 * recorridos, va el título y una card por recorrido. No tiene «+».
 *
 * @param onRunClick recibe el id del recorrido tocado.
 */
@Composable
fun ProfileContent(
    state: ProfileUiState,
    onSettingsClick: () -> Unit,
    onRunClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        TrazoTopBar(
            title = stringResource(R.string.app_name),
            onSettingsClick = onSettingsClick,
        )

        LazyColumn(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(key = "stats") { ProfileStats(state = state) }

            if (state.hasRecentRuns) {
                item(key = "runs-title") {
                    ProfileSectionTitle(
                        text = stringResource(R.string.profile_runs_title),
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                }
                items(items = state.runs, key = { it.id }) { run ->
                    RunCard(run = run, onClick = { onRunClick(run.id) })
                }
            } else {
                item(key = "empty") {
                    ProfileSectionCard(title = stringResource(R.string.profile_runs_title)) {
                        EmptyState(
                            icon = Icons.AutoMirrored.Filled.DirectionsRun,
                            title = stringResource(R.string.profile_empty_runs),
                            body = stringResource(R.string.profile_empty_runs_body),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.sm),
                        )
                    }
                }
            }
        }
    }
}

/** Card «Mis estadísticas»: tres barras con su valor y etiqueta. */
@Composable
private fun ProfileStats(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
) {
    ProfileSectionCard(
        title = stringResource(R.string.profile_stats_title),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            ProfileStatColumn(
                label = stringResource(R.string.profile_stats_runs_label),
                value = state.totals.runCount.toString(),
                fill = state.statsFill.runs,
                barColor = TrazoTheme.extendedColors.statBarRuns,
                modifier = Modifier.weight(1f),
            )
            ProfileStatColumn(
                label = stringResource(R.string.profile_stats_distance_label),
                value =
                    stringResource(
                        R.string.profile_stats_distance,
                        RunValueFormatter.formatTotalDistanceKm(state.totals.distanceKm),
                    ),
                fill = state.statsFill.distance,
                barColor = TrazoTheme.extendedColors.statBarDistance,
                modifier = Modifier.weight(1f),
            )
            ProfileStatColumn(
                label = stringResource(R.string.profile_stats_time_label),
                value = RunValueFormatter.formatDuration(state.totals.timeSeconds),
                fill = state.statsFill.time,
                barColor = TrazoTheme.extendedColors.statBarTime,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Título de sección, marcado como encabezado para los lectores de pantalla. */
@Composable
private fun ProfileSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.semantics { heading() },
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ProfileContentEmptyPreview() {
    TrazoTheme {
        ProfileContent(
            state = ProfileUiState(),
            onSettingsClick = {},
            onRunClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ProfileContentWithRunsPreview() {
    TrazoTheme {
        ProfileContent(
            state =
                ProfileUiState(
                    runs =
                        listOf(
                            RunCardUi(
                                id = "2",
                                title = "Vuelta al lago",
                                dateText = "Mar 28 oct",
                                distanceText = "5,2 km",
                                durationText = "00:55:00",
                                brakeCount = 3,
                                regularityPercent = 80,
                                badges = emptyList(),
                            ),
                            RunCardUi(
                                id = "1",
                                title = "Costanera",
                                dateText = "Dom 26 oct",
                                distanceText = "2,8 km",
                                durationText = "00:00:00",
                                brakeCount = 0,
                                regularityPercent = null,
                                badges = listOf(StatusBadgeKind.INCOMPLETE, StatusBadgeKind.DEMO),
                            ),
                        ),
                    totals = SavedRunTotals(runCount = 1, distanceKm = 5.2, timeSeconds = 3_300L),
                    statsFill = ProfileStatsFill(runs = 0.05f, distance = 0.1f, time = 0.09f),
                ),
            onSettingsClick = {},
            onRunClick = {},
        )
    }
}
