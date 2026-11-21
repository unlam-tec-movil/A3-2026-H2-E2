package com.trazo.app.ui.navigation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.trazo.app.R
import com.trazo.app.ui.components.common.LocationIssueDialog
import com.trazo.app.ui.components.common.StartRunDialog
import com.trazo.app.ui.components.common.StartRunFab
import com.trazo.app.ui.screens.map.MapScreen
import com.trazo.app.ui.screens.profile.ProfileScreen
import com.trazo.app.ui.screens.result.ResultScreen
import com.trazo.app.ui.screens.startrun.StartRunEffect
import com.trazo.app.ui.screens.startrun.StartRunViewModel
import com.trazo.app.ui.screens.startrun.components.GpsWaitingNotice
import kotlin.math.roundToInt

@Composable
fun TrazoApp() {
    val navController = rememberNavController()
    val startRunViewModel: StartRunViewModel = hiltViewModel()
    val startRunState by startRunViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            startRunViewModel.onPermissionResult()
        }

    // La lógica del inicio vive en StartRunViewModel; acá solo se ejecutan sus efectos.
    LaunchedEffect(startRunViewModel) {
        startRunViewModel.uiEffect.collect { effect ->
            when (effect) {
                StartRunEffect.RequestLocationPermission ->
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                StartRunEffect.NavigateToActivity -> navController.navigateToTab(TrazoDestination.ACTIVITY)
            }
        }
    }

    // El «+» solo vive en Actividad (start-run, requisito 9).
    val startRunFab: @Composable (Modifier) -> Unit = { fabModifier ->
        StartRunFab(
            enabled = startRunState.isFabEnabled,
            isOptionVisible = startRunState.isOptionVisible,
            onFabClick = startRunViewModel::onFabClick,
            onOptionDismiss = startRunViewModel::onOptionDismiss,
            onOptionClick = startRunViewModel::onOptionClick,
            modifier = fabModifier,
        )
    }

    val gpsWaitingNotice: @Composable (Modifier) -> Unit = { noticeModifier ->
        startRunState.waitingGoal?.let { goal ->
            GpsWaitingNotice(
                goalLabel = stringResource(R.string.start_run_goal_km, goal.distanceKm.roundToInt()),
                onCancel = startRunViewModel::onCancelWaiting,
                modifier = noticeModifier,
            )
        }
    }

    TrazoScaffold(
        navController = navController,
        activityScreen = { onNavigateToResult ->
            MapScreen(
                onStartRunClick = startRunViewModel::onOptionClick,
                onNavigateToResults = { summary -> onNavigateToResult(summary.id) },
                startRunFab = startRunFab,
                gpsWaitingNotice = gpsWaitingNotice,
            )
        },
        profileScreen = { ProfileScreen(onSettingsClick = {}) },
        resultScreen = { onLeaveToActivity ->
            ResultScreen(
                // Parche temporal (aprobado por Rocío): hasta que save-run (HU-07) publique
                // saverun/{runId}, «Continuar» vuelve a Actividad. Reemplazar por navigate("saverun/$runId").
                onContinue = { _ -> onLeaveToActivity() },
                onLeave = onLeaveToActivity,
            )
        },
    )

    if (startRunState.isDialogVisible) {
        StartRunDialog(
            selectedGoal = startRunState.selectedGoal,
            onGoalSelected = startRunViewModel::onGoalSelected,
            onConfirm = startRunViewModel::onConfirm,
            onDismiss = startRunViewModel::onDismiss,
        )
    }

    startRunState.locationIssue?.let { issue ->
        LocationIssueDialog(
            issue = issue,
            onOpenSettings = {
                context.startActivity(locationSettingsIntent(context, issue))
                startRunViewModel.onLocationIssueDismiss()
            },
            onDismiss = startRunViewModel::onLocationIssueDismiss,
        )
    }
}
