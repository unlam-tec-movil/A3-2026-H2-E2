package com.trazo.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.trazo.app.ui.screens.history.HistoryScreen
import com.trazo.app.ui.screens.home.HomeScreen
import com.trazo.app.ui.screens.map.MapScreen

@Composable
fun TrazoApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onDestinationSelected = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TrazoDestination.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TrazoDestination.HOME.route) { HomeScreen() }
            composable(TrazoDestination.MAP.route) {
                MapScreen(
                    onStartRunClick = {
                        // TODO (Persona 2): Abrir modal o flujo de inicio 'start-run'
                    },
                    onNavigateToResults = { _ ->
                        // TODO (Persona 4): Navegar a la pantalla de Resultados pasando el resumen congelado
                    },
                )
            }
            composable(TrazoDestination.HISTORY.route) { HistoryScreen() }
        }
    }
}
