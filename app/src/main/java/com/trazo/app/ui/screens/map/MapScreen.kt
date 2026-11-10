package com.trazo.app.ui.screens.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.LatLng
import com.trazo.app.ui.components.layout.FinishConfirmDialog
import com.trazo.app.ui.components.layout.GpsIndicator
import com.trazo.app.ui.components.layout.MapLayer
import com.trazo.app.ui.components.layout.RunControls
import com.trazo.app.ui.components.layout.RunPanel
import com.trazo.app.ui.theme.Spacing

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: LiveRouteTrackingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()

    var showFinishConfirmDialog by remember {
        mutableStateOf(false)
    }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val hasPermission =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (hasPermission) {
                viewModel.refreshCurrentLocation()
            }
        }

    LaunchedEffect(Unit) {
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

        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.refreshCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    val currentLocation =
        uiState.currentLocation?.let { location ->
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
            )

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
                        viewModel.pauseRun()
                    },
                    onResume = {
                        viewModel.resumeRun()
                    },
                    onFinish = {
                        showFinishConfirmDialog = true
                    },
                )
            }

            FinishConfirmDialog(
                showFinishConfirmDialog = showFinishConfirmDialog,
                onConfirmFinish = {
                    showFinishConfirmDialog = false
                    viewModel.finishRun()
                },
                onCancelFinish = {
                    showFinishConfirmDialog = false
                },
            )
        }
    }
}
