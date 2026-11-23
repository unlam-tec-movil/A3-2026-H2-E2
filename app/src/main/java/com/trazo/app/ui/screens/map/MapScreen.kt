package com.trazo.app.ui.screens.map

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.trazo.app.R
import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.ui.screens.map.components.FinishConfirmDialog
import com.trazo.app.ui.screens.map.components.GoalCelebrationOverlay
import com.trazo.app.ui.screens.map.components.GpsIndicator
import com.trazo.app.ui.screens.map.components.MapLayer
import com.trazo.app.ui.screens.map.components.MotionSensorMissingNotice
import com.trazo.app.ui.screens.map.components.RunControls
import com.trazo.app.ui.screens.map.components.RunPanel
import com.trazo.app.ui.theme.Spacing

@Composable
fun MapScreen(
    onStartRunClick: () -> Unit,
    onNavigateToResults: (FrozenRunSummary) -> Unit,
    modifier: Modifier = Modifier,
    onSavedRunClick: (String) -> Unit = {},
    shortRunNotice: (@Composable (onDiscard: () -> Unit, onReturn: () -> Unit) -> Unit)? = null,
    // Piezas de start-run armadas por TrazoApp: el botón «+» y el aviso «Esperando señal GPS».
    startRunFab: @Composable (Modifier) -> Unit = {},
    gpsWaitingNotice: @Composable (Modifier) -> Unit = {},
    // Pieza de stop-detection armada por TrazoApp: alerta de frenada y consulta. Con `suppressed` no se ve.
    stopDetectionOverlay: @Composable (suppressed: Boolean, Modifier) -> Unit = { _, _ -> },
    viewModel: LiveRouteTrackingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val permissionLostMessage = stringResource(R.string.map_location_permission_lost)

    DisposableEffect(activity, uiState.hasActiveRun) {
        if (activity != null && uiState.hasActiveRun) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Al volver a primer plano se relee la ubicación: de eso depende que se dibujen las trazas guardadas.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onLocationStatusRefreshed() }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is LiveRouteTrackingUiEffect.NavigateToResults -> {
                    onNavigateToResults(effect.summary)
                }

                is LiveRouteTrackingUiEffect.OpenStartRunModal -> {
                    onStartRunClick()
                }

                is LiveRouteTrackingUiEffect.OpenSavedRun -> {
                    onSavedRunClick(effect.runId)
                }

                is LiveRouteTrackingUiEffect.ShowLocationPermissionLostMessage -> {
                    snackbarHostState.showSnackbar(permissionLostMessage, duration = SnackbarDuration.Long)
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

    val savedTraces = remember(uiState.savedTraces) { uiState.savedTraces.toMapLayerTraces() }

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
            savedTraces = savedTraces,
            onMapTap = viewModel::onMapTapped,
            // El «+» va entre «Centrar» y el panel del temporizador.
            belowCenterButton = { startRunFab(Modifier) },
        ) {
            gpsWaitingNotice(Modifier.padding(bottom = Spacing.md))
            stopDetectionOverlay(
                uiState.suppressed,
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.md),
            )
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
                if (uiState.showMotionSensorMissing) {
                    MotionSensorMissingNotice(modifier = Modifier.padding(top = Spacing.sm))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(Spacing.lg),
        )

        FinishConfirmDialog(
            showFinishConfirmDialog = uiState.showFinishConfirmDialog,
            goalReached = uiState.goalReached,
            onConfirmFinish = {
                viewModel.onConfirmFinish()
            },
            onCancelFinish = {
                viewModel.onCancelFinish()
            },
            finishFailed = uiState.finishFailed,
            isFinishing = uiState.isFinishing,
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
            onFinish = {
                viewModel.onCelebrationDismissed()
                viewModel.onFinishRequested()
            },
        )
    }
}

/** Trazas del estado en el formato que recibe [MapLayer]: id del recorrido a sus puntos. */
internal fun List<SavedTraceUi>.toMapLayerTraces(): Map<String, List<RoutePoint>> = associate { it.id to it.points }
