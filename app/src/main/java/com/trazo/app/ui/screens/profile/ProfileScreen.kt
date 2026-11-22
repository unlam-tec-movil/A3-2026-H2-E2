package com.trazo.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.app.R
import com.trazo.app.ui.components.common.RunValueFormatter
import com.trazo.app.ui.components.common.StatusBadgeKind
import com.trazo.app.ui.screens.profile.components.EmptyState
import com.trazo.app.ui.screens.profile.components.ProfileStatRow
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
 * Arriba va la barra superior con "Trazo" y Configuración; debajo, en una lista perezosa, «Mis
 * estadísticas», el título «Mis recorridos» y una card por recorrido. Mientras no hay recorridos
 * se muestra el estado vacío y los totales quedan en cero. No tiene «+».
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

            item(key = "runs-title") {
                ProfileSectionTitle(text = stringResource(R.string.profile_runs_title))
            }

            if (state.hasRecentRuns) {
                items(items = state.runs, key = { it.id }) { run ->
                    RunCard(run = run, onClick = { onRunClick(run.id) })
                }
            } else {
                item(key = "empty") {
                    // EmptyState ya aplica el margen lateral de pantalla.
                    EmptyState(
                        illustrationRes = R.drawable.img_empty_runs,
                        message = stringResource(R.string.profile_empty_runs),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.xl),
                    )
                }
            }
        }
    }
}

/** «Mis estadísticas»: título y las tres filas de totales. */
@Composable
private fun ProfileStats(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = Spacing.md)) {
        ProfileSectionTitle(text = stringResource(R.string.profile_stats_title))
        Spacer(modifier = Modifier.height(Spacing.sm))
        ProfileStatRow(
            label = stringResource(R.string.profile_stats_runs_label),
            value = pluralStringResource(R.plurals.profile_stats_runs, state.totalRuns, state.totalRuns),
        )
        ProfileStatRow(
            label = stringResource(R.string.profile_stats_distance_label),
            value =
                stringResource(
                    R.string.profile_stats_distance,
                    RunValueFormatter.formatTotalDistanceKm(state.totalDistanceKm),
                ),
        )
        ProfileStatRow(
            label = stringResource(R.string.profile_stats_time_label),
            value = RunValueFormatter.formatDuration(state.totalTimeSeconds),
        )
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
                    totalRuns = 1,
                    totalDistanceKm = 5.2,
                    totalTimeSeconds = 3_300L,
                ),
            onSettingsClick = {},
            onRunClick = {},
        )
    }
}
