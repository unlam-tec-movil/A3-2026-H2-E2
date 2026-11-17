package com.trazo.app.ui.screens.map

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.trazo.app.R
import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.ui.components.layout.FinishConfirmDialog
import com.trazo.app.ui.components.layout.GoalCelebrationOverlay
import com.trazo.app.ui.components.layout.GpsIndicator
import com.trazo.app.ui.components.layout.MapLayer
import com.trazo.app.ui.components.layout.RunControls
import com.trazo.app.ui.components.layout.RunPanel
import com.trazo.app.ui.theme.Spacing

@Composable
fun MapScreen(
    onStartRunClick: () -> Unit,
    onNavigateToResults: (FrozenRunSummary) -> Unit,
    modifier: Modifier = Modifier,
    shortRunNotice: (@Composable (onDiscard: () -> Unit, onReturn: () -> Unit) -> Unit)? = null,
    viewModel: LiveRouteTrackingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(activity, uiState.hasActiveRun) {
        if (activity != null && uiState.hasActiveRun) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is LiveRouteTrackingUiEffect.NavigateToResults -> {
                    onNavigateToResults(effect.summary)
                }

                is LiveRouteTrackingUiEffect.OpenStartRunModal -> {
                    onStartRunClick()
                }

                is LiveRouteTrackingUiEffect.ReturnToIdleMap -> {
                    // Retorno a estado de mapa sin carrera
                }

                is LiveRouteTrackingUiEffect.ShowLocationPermissionLostMessage -> {
                    // Aviso de pérdida de permisos de ubicación
                }
            }
        }
    }

    // Íconos oscuros en la barra de estado mientras se ve el mapa claro (Inicio usa claros).
    val view = LocalView.current
    DisposableEffect(activity, view) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, view) }
        val previousLightStatusBars = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = true
        onDispose {
            if (controller != null && previousLightStatusBars != null) {
                controller.isAppearanceLightStatusBars = previousLightStatusBars
            }
        }
    }

    // Sin Scaffold propio: el mapa ocupa también el área de la barra de estado, y el espacio de la
    // barra inferior ya lo reserva el Scaffold de TrazoApp.
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        MapLayer(
            routePoints = uiState.routePoints,
            currentLocation = uiState.runnerPosition,
            isMapCenteredOnRunner = uiState.isMapCenteredOnRunner,
            onCenterMapClicked = { viewModel.onCenterMapClicked() },
            onMapDragged = { viewModel.onMapDragged() },
        ) {
            RunPanel(uiState = uiState) {
                RunControls(
                    uiState = uiState,
                    onPause = viewModel::onPauseClicked,
                    onResume = viewModel::onResumeClicked,
                    onFinish = viewModel::onFinishRequested,
                )
            }
        }

        if (uiState.hasActiveRun) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(Spacing.lg),
            ) {
                GpsIndicator(
                    gpsQuality = uiState.gpsQuality,
                )
            }
        }

        FinishConfirmDialog(
            showFinishConfirmDialog = uiState.showFinishConfirmDialog,
            goalReached = uiState.goalReached,
            onConfirmFinish = {
                viewModel.onConfirmFinish()
            },
            onCancelFinish = {
                viewModel.onCancelFinish()
            },
        )

        if (uiState.showShortRunDialog) {
            if (shortRunNotice != null) {
                shortRunNotice(
                    { viewModel.onDiscardShortRun() },
                    { viewModel.onReturnFromShortRun() },
                )
            } else {
                AlertDialog(
                    onDismissRequest = { viewModel.onReturnFromShortRun() },
                    title = { Text(text = stringResource(R.string.map_short_run_title)) },
                    text = { Text(text = stringResource(R.string.map_short_run_text)) },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.onDiscardShortRun() },
                        ) {
                            Text(
                                text = stringResource(R.string.map_discard),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { viewModel.onReturnFromShortRun() },
                        ) {
                            Text(text = stringResource(R.string.map_return_to_run))
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            }
        }

        GoalCelebrationOverlay(
            showCelebration = uiState.showGoalCelebration,
            goal = uiState.goal,
            onDismiss = {
                viewModel.onCelebrationDismissed()
            },
        )
    }
}
