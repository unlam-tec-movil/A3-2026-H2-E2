package com.trazo.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.app.R
import com.trazo.app.ui.screens.home.components.EmptyState
import com.trazo.app.ui.screens.home.components.StartRunFab
import com.trazo.app.ui.screens.home.components.TrazoTopBar
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

/**
 * Conecta [HomeViewModel] con [HomeContent]. No dibuja nada propio.
 *
 * @param onStartRunClick se invoca al elegir "Iniciar un recorrido", después de cerrar la opción.
 * @param onSettingsClick se invoca al tocar el botón de Configuración de la barra superior.
 */
@Composable
fun HomeScreen(
    onStartRunClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onSettingsClick = onSettingsClick,
        onStartRunFabClick = viewModel::onStartRunFabClick,
        onStartRunOptionDismiss = viewModel::onStartRunOptionDismiss,
        onStartRunOptionClick = {
            viewModel.onStartRunOptionClick()
            onStartRunClick()
        },
        modifier = modifier,
    )
}

/**
 * Contenido de Inicio. Se dibuja solo a partir de [state]; no guarda estado propio.
 * Arriba va la barra superior con "Trazo" y Configuración. Sin recorridos muestra el estado
 * vacío; con recorridos, en esta versión no dibuja cards.
 */
@Composable
fun HomeContent(
    state: HomeUiState,
    onSettingsClick: () -> Unit,
    onStartRunFabClick: () -> Unit,
    onStartRunOptionDismiss: () -> Unit,
    onStartRunOptionClick: () -> Unit,
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

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        ) {
            if (!state.hasRecentRuns) {
                // EmptyState ya aplica el margen lateral de pantalla.
                EmptyState(
                    illustrationRes = R.drawable.img_empty_runs,
                    message = stringResource(R.string.home_empty_runs),
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            StartRunFab(
                enabled = state.isStartRunEnabled,
                isOptionVisible = state.isStartRunOptionVisible,
                onFabClick = onStartRunFabClick,
                onOptionDismiss = onStartRunOptionDismiss,
                onOptionClick = onStartRunOptionClick,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(Spacing.lg),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun HomeContentEmptyPreview() {
    TrazoTheme {
        HomeContent(
            state = HomeUiState(),
            onSettingsClick = {},
            onStartRunFabClick = {},
            onStartRunOptionDismiss = {},
            onStartRunOptionClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun HomeContentOptionVisiblePreview() {
    TrazoTheme {
        HomeContent(
            state = HomeUiState(isStartRunOptionVisible = true),
            onSettingsClick = {},
            onStartRunFabClick = {},
            onStartRunOptionDismiss = {},
            onStartRunOptionClick = {},
        )
    }
}
