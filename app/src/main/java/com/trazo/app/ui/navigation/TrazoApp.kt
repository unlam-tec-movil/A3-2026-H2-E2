package com.trazo.app.ui.navigation

import android.Manifest
import android.net.http.SslCertificate.restoreState
import android.net.http.SslCertificate.saveState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.trazo.app.data.location.locationSettingsIntent
import com.trazo.app.data.location.readLocationStatus
import com.trazo.app.domain.model.LocationStatus
import com.trazo.app.ui.components.common.LocationIssueDialog
import com.trazo.app.ui.components.common.StartRunDialog
import com.trazo.app.ui.screens.history.HistoryScreen
import com.trazo.app.ui.screens.home.HomeScreen
import com.trazo.app.ui.screens.map.MapScreen
import com.trazo.app.ui.screens.startrun.StartRunViewModel

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
            startRunViewModel.onStartRunRequested(readLocationStatus(context))
        }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // El inset de la barra de estado no se consume acá: lo maneja cada pantalla
        // (Inicio lo cubre con su barra superior).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onDestinationSelected = navigateTo,
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TrazoDestination.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TrazoDestination.HOME.route) {
                HomeScreen(
                    onStartRunClick = {
                        val status = readLocationStatus(context)
                        if (status == LocationStatus.PERMISSION_DENIED) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        } else {
                            startRunViewModel.onStartRunRequested(status)
                        }
                    },
                    onSettingsClick = {},
                )
            }
            composable(TrazoDestination.MAP.route) { MapScreen() }
            composable(TrazoDestination.HISTORY.route) { HistoryScreen() }
        }
    }

    if (startRunState.isDialogVisible) {
        StartRunDialog(
            selectedGoal = startRunState.selectedGoal,
            onGoalSelected = startRunViewModel::onGoalSelected,
            onConfirm = {
                if (startRunViewModel.onConfirm() != null) {
                    navigateTo(TrazoDestination.MAP)
                }
            },
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
