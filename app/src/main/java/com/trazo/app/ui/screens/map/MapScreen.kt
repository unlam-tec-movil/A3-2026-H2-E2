package com.trazo.app.ui.screens.map

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.model.LatLng
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
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(activity, uiState.hasActiveRun) {
        if (activity != null && uiState.hasActiveRun) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(lifecycleOwner, uiState.hasActiveRun) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && uiState.hasActiveRun) {
                    val fineLocationGranted =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED

                    val coarseLocationGranted =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED

                    if (!fineLocationGranted && !coarseLocationGranted) {
                        viewModel.onLocationPermissionRevoked()
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                    )
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        title = { Text(text = "Recorrido demasiado corto") },
                        text = { Text(text = "Un recorrido de menos de 200 metros no se puede guardar. ¿Deseás descartarlo?") },
                        confirmButton = {
                            TextButton(
                                onClick = { viewModel.onDiscardShortRun() },
                            ) {
                                Text(
                                    text = "Descartar",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { viewModel.onReturnFromShortRun() },
                            ) {
                                Text(text = "Volver al recorrido")
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
