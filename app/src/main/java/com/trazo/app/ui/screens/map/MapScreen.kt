package com.trazo.app.ui.screens.map

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.LatLng
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

    val currentLocation =
        uiState.runnerPosition?.let { location ->
            LatLng(
                location.latitude,
                location.longitude,
            )
        }

    Scaffold(
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            MapLayer(
                routePoints = uiState.routePoints,
                currentLocation = currentLocation,
                isMapCenteredOnRunner = uiState.isMapCenteredOnRunner,
                onCenterMapClicked = { viewModel.onCenterMapClicked() },
                onMapDragged = { viewModel.onMapDragged() },
            )

            if (uiState.hasActiveRun) {
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(Spacing.lg),
                ) {
                    GpsIndicator(
                        gpsQuality = uiState.gpsQuality,
                    )
                }
            }

            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                RunPanel(
                    uiState = uiState,
                )

                RunControls(
                    uiState = uiState,
                    onPause = {
                        viewModel.onPauseClicked()
                    },
                    onResume = {
                        viewModel.onResumeClicked()
                    },
                    onFinish = {
                        viewModel.onFinishRequested()
                    },
                )
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
}
