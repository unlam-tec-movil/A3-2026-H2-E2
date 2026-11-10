package com.trazo.app.ui.navigation

import android.net.http.SslCertificate.restoreState
import android.net.http.SslCertificate.saveState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.trazo.app.domain.model.LocationStatus
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
                // El modal "Iniciar nuevo recorrido" y la pantalla de Configuración
                // pertenecen a otras historias.
                HomeScreen(
                    // Provisorio: falta consultar el permiso y el estado real de la ubicación.
                    onStartRunClick = { startRunViewModel.onStartRunRequested(LocationStatus.READY) },
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
}
