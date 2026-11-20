package com.trazo.app.ui.navigation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trazo.app.R
import com.trazo.app.ui.components.common.LocationIssueDialog
import com.trazo.app.ui.components.common.StartRunDialog
import com.trazo.app.ui.components.common.StartRunFab
import com.trazo.app.ui.screens.home.HomeScreen
import com.trazo.app.ui.screens.map.MapScreen
import com.trazo.app.ui.screens.result.ResultScreen
import com.trazo.app.ui.screens.startrun.StartRunEffect
import com.trazo.app.ui.screens.startrun.StartRunViewModel
import com.trazo.app.ui.screens.startrun.components.GpsWaitingNotice
import kotlin.math.roundToInt

private const val RESULT_ROUTE = "result/{runId}"

@Composable
fun TrazoApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val startRunViewModel: StartRunViewModel = hiltViewModel()
    val startRunState by startRunViewModel.uiState.collectAsStateWithLifecycle()
    val navigateTo: (TrazoDestination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
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
                StartRunEffect.NavigateToActivity -> navigateTo(TrazoDestination.ACTIVITY)
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // El inset de la barra de estado no se consume acá: lo maneja cada pantalla
        // (Perfil lo cubre con su barra superior).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute != RESULT_ROUTE) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onDestinationSelected = navigateTo,
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TrazoDestination.ACTIVITY.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TrazoDestination.ACTIVITY.route) {
                MapScreen(
                    onStartRunClick = startRunViewModel::onOptionClick,
                    onNavigateToResults = { summary ->
                        navController.navigate("result/${summary.id}") { launchSingleTop = true }
                    },
                    startRunFab = startRunFab,
                    gpsWaitingNotice = gpsWaitingNotice,
                )
            }
            composable(TrazoDestination.PROFILE.route) {
                HomeScreen(
                    startRunFab = {},
                    onSettingsClick = {},
                )
            }
            composable(
                route = RESULT_ROUTE,
                arguments = listOf(navArgument("runId") { type = NavType.StringType }),
            ) {
                val leaveToActivity: () -> Unit = {
                    navController.navigate(TrazoDestination.ACTIVITY.route) {
                        popUpTo(RESULT_ROUTE) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                ResultScreen(
                    // Parche temporal (aprobado por Rocío): hasta que save-run (HU-07) publique
                    // saverun/{runId}, «Continuar» vuelve a Actividad. Reemplazar por navigate("saverun/$runId").
                    onContinue = { _ -> leaveToActivity() },
                    onLeave = leaveToActivity,
                )
            }
        }
    }

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
