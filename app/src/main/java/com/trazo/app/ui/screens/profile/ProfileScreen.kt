package com.trazo.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.trazo.app.ui.screens.profile.components.EmptyState
import com.trazo.app.ui.screens.profile.components.ProfileStatRow
import com.trazo.app.ui.screens.profile.components.TrazoTopBar
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

/**
 * Conecta [ProfileViewModel] con [ProfileContent]. No dibuja nada propio.
 *
 * @param onSettingsClick se invoca al tocar el botón de Configuración de la barra superior.
 */
@Composable
fun ProfileScreen(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileContent(
        state = state,
        onSettingsClick = onSettingsClick,
        modifier = modifier,
    )
}

/**
 * Contenido de Perfil. Se dibuja solo a partir de [state]; no guarda estado propio.
 * Arriba va la barra superior con "Trazo" y Configuración; debajo, con scroll, «Mis estadísticas»
 * y «Mis recorridos». Sin recorridos, «Mis recorridos» muestra el estado vacío. No tiene «+».
 */
@Composable
fun ProfileContent(
    state: ProfileUiState,
    onSettingsClick: () -> Unit,
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

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
        ) {
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
                        ProfileStatsFormatter.formatDistanceKm(state.totalDistanceKm),
                    ),
            )
            ProfileStatRow(
                label = stringResource(R.string.profile_stats_time_label),
                value = ProfileStatsFormatter.formatTotalTime(state.totalTimeSeconds),
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            ProfileSectionTitle(text = stringResource(R.string.profile_runs_title))
            if (!state.hasRecentRuns) {
                Spacer(modifier = Modifier.height(Spacing.xl))
                // EmptyState ya aplica el margen lateral de pantalla.
                EmptyState(
                    illustrationRes = R.drawable.img_empty_runs,
                    message = stringResource(R.string.profile_empty_runs),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ProfileContentWithTotalsPreview() {
    TrazoTheme {
        ProfileContent(
            state = ProfileUiState(totalRuns = 2, totalDistanceKm = 8.0, totalTimeSeconds = 3_300L),
            onSettingsClick = {},
        )
    }
}
