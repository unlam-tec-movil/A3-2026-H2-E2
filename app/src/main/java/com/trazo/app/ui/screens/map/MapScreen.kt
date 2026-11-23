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

/**
 * Pestaña Actividad: conecta el ViewModel, los efectos y la ventana (pantalla encendida, barra de
 * estado). Lo que se dibuja está en [MapContent].
 *
 * Las piezas de otras specs llegan armadas desde `TrazoApp` (slots), así esta pantalla no las conoce:
 * el botón «+» y el aviso «Esperando señal GPS» (start-run) y la alerta de frenada (stop-detection),
 * que con `suppressed` no se ve.
 */
@Composable
fun MapScreen(
    onNavigateToResults: (FrozenRunSummary) -> Unit,
    modifier: Modifier = Modifier,
    onSavedRunClick: (String) -> Unit = {},
    startRunFab: @Composable (Modifier) -> Unit = {},
    gpsWaitingNotice: @Composable (Modifier) -> Unit = {},
    stopDetectionOverlay: @Composable (suppressed: Boolean, Modifier) -> Unit = { _, _ -> },
    viewModel: LiveRouteTrackingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val permissionLostMessage = stringResource(R.string.map_location_permission_lost)

    KeepScreenOn(enabled = uiState.hasActiveRun)
    DarkStatusBarIcons()

    // Al volver a primer plano se relee la ubicación: de eso depende que se dibujen las trazas guardadas.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onLocationStatusRefreshed() }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is LiveRouteTrackingUiEffect.NavigateToResults -> onNavigateToResults(effect.summary)
                is LiveRouteTrackingUiEffect.OpenSavedRun -> onSavedRunClick(effect.runId)
                is LiveRouteTrackingUiEffect.ShowLocationPermissionLostMessage ->
                    snackbarHostState.showSnackbar(permissionLostMessage, duration = SnackbarDuration.Long)
            }
        }
    }

    val actions =
        remember(viewModel) {
            MapActions(
                onCenterMap = viewModel::onCenterMapClicked,
                onMapDragged = viewModel::onMapDragged,
                onMapTap = viewModel::onMapTapped,
                onPause = viewModel::onPauseClicked,
                onResume = viewModel::onResumeClicked,
                onFinish = viewModel::onFinishRequested,
                onConfirmFinish = viewModel::onConfirmFinish,
                onCancelFinish = viewModel::onCancelFinish,
                onDiscardShortRun = viewModel::onDiscardShortRun,
                onReturnFromShortRun = viewModel::onReturnFromShortRun,
                onCelebrationDismissed = viewModel::onCelebrationDismissed,
            )
        }

    MapContent(
        uiState = uiState,
        actions = actions,
        snackbarHostState = snackbarHostState,
        startRunFab = startRunFab,
        gpsWaitingNotice = gpsWaitingNotice,
        stopDetectionOverlay = stopDetectionOverlay,
        modifier = modifier,
    )
}

/** Lo que el usuario puede hacer en el Mapa. Lo arma [MapScreen] con los métodos del ViewModel. */
internal class MapActions(
    val onCenterMap: () -> Unit,
    val onMapDragged: () -> Unit,
    /** Toque sobre el mapa, para abrir la traza guardada que quede cerca. */
    val onMapTap: (latitude: Double, longitude: Double, toleranceMeters: Double) -> Unit,
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onFinish: () -> Unit,
    val onConfirmFinish: () -> Unit,
    val onCancelFinish: () -> Unit,
    val onDiscardShortRun: () -> Unit,
    val onReturnFromShortRun: () -> Unit,
    val onCelebrationDismissed: () -> Unit,
)

/** Mapa sin estado propio: dibuja [uiState] y avisa cada acción por [actions]. */
@Composable
internal fun MapContent(
    uiState: LiveRouteTrackingUiState,
    actions: MapActions,
    snackbarHostState: SnackbarHostState,
    startRunFab: @Composable (Modifier) -> Unit,
    gpsWaitingNotice: @Composable (Modifier) -> Unit,
    stopDetectionOverlay: @Composable (suppressed: Boolean, Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val savedTraces = remember(uiState.savedTraces) { uiState.savedTraces.toMapLayerTraces() }

    // Sin Scaffold propio: el mapa ocupa también el área de la barra de estado, y el espacio de la
    // barra inferior ya lo reserva el Scaffold de TrazoApp.
    Box(modifier = modifier.fillMaxSize()) {
        MapLayer(
            routePoints = uiState.routePoints,
            currentLocation = uiState.runnerPosition,
            isMapCenteredOnRunner = uiState.isMapCenteredOnRunner,
            onCenterMapClicked = actions.onCenterMap,
            onMapDragged = actions.onMapDragged,
            savedTraces = savedTraces,
            onMapTap = actions.onMapTap,
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
                    onPause = actions.onPause,
                    onResume = actions.onResume,
                    onFinish = actions.onFinish,
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
                GpsIndicator(gpsQuality = uiState.gpsQuality)
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
            onConfirmFinish = actions.onConfirmFinish,
            onCancelFinish = actions.onCancelFinish,
            finishFailed = uiState.finishFailed,
            isFinishing = uiState.isFinishing,
        )

        if (uiState.showShortRunDialog) {
            ShortRunDialog(onDiscard = actions.onDiscardShortRun, onReturn = actions.onReturnFromShortRun)
        }

        GoalCelebrationOverlay(
            showCelebration = uiState.showGoalCelebration,
            goal = uiState.goal,
            onDismiss = actions.onCelebrationDismissed,
            onFinish = {
                actions.onCelebrationDismissed()
                actions.onFinish()
            },
        )
    }
}

/** Recorrido de menos de 200 m: se puede descartar o volver al recorrido. */
@Composable
private fun ShortRunDialog(
    onDiscard: () -> Unit,
    onReturn: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onReturn,
        title = { Text(text = stringResource(R.string.map_short_run_title)) },
        text = { Text(text = stringResource(R.string.map_short_run_text)) },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(
                    text = stringResource(R.string.map_discard),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onReturn) {
                Text(text = stringResource(R.string.map_return_to_run))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

/** Con un recorrido en curso, la pantalla no se apaga sola. */
@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity, enabled) {
        if (activity != null && enabled) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

/** Íconos oscuros en la barra de estado mientras se ve el mapa claro; al salir se restauran. */
@Composable
private fun DarkStatusBarIcons() {
    val activity = LocalContext.current as? Activity
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
}

/** Trazas del estado en el formato que recibe [MapLayer]: id del recorrido a sus puntos. */
internal fun List<SavedTraceUi>.toMapLayerTraces(): Map<String, List<RoutePoint>> = associate { it.id to it.points }
