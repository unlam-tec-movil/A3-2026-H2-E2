package com.trazo.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.trazo.app.R
import com.trazo.app.ui.components.common.SuccessNotice
import kotlinx.coroutines.launch

internal const val RESULT_ROUTE = "result/{runId}"
internal const val SETTINGS_ROUTE = "settings"
internal const val SAVE_RUN_ROUTE = "saverun/{runId}"

/** Cambia de pestaña guardando y restaurando el estado de cada una, sin apilar pestañas repetidas. */
internal fun NavController.navigateToTab(destination: TrazoDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Esqueleto de navegación: barra inferior y NavHost. No conoce ViewModels: cada pantalla llega
 * como slot, así la navegación se prueba con pantallas falsas.
 *
 * - Abre en Actividad.
 * - La barra inferior se oculta en el resultado y en la configuración.
 * - Salir del resultado vuelve a Actividad y saca el resultado de la pila.
 * - Guardar un recorrido lleva a Perfil, saca el resultado y el guardado de la pila y muestra el
 *   aviso «Recorrido guardado.» (save-run 5.3 y 7.6). Cerrar «Guardar recorrido» vuelve a Actividad.
 *
 * @param activityScreen pestaña Actividad; recibe cómo abrir el resultado de un recorrido (`runId`).
 * @param profileScreen pestaña Perfil; recibe cómo abrir la configuración (`onSettingsClick`).
 * @param resultScreen resultado del recorrido; recibe cómo volver a Actividad.
 * @param settingsScreen pantalla Configuración; recibe cómo volver a la pantalla anterior.
 * @param saveRunScreen pantalla Guardar recorrido; recibe qué hacer al guardar (`onSaved`) y al cerrar (`onClose`).
 */
@Composable
internal fun TrazoScaffold(
    navController: NavHostController,
    activityScreen: @Composable (onNavigateToResult: (runId: String) -> Unit) -> Unit,
    profileScreen: @Composable (onSettingsClick: () -> Unit) -> Unit,
    resultScreen: @Composable (onLeaveToActivity: () -> Unit) -> Unit,
    saveRunScreen: @Composable (onSaved: () -> Unit, onClose: () -> Unit) -> Unit,
    settingsScreen: @Composable (onBackClick: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedNotice = stringResource(R.string.save_run_saved_notice)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                SuccessNotice(text = data.visuals.message)
            }
        },
        bottomBar = {
            if (currentRoute != RESULT_ROUTE && currentRoute != SETTINGS_ROUTE && currentRoute != SAVE_RUN_ROUTE) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onDestinationSelected = navController::navigateToTab,
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
                activityScreen { runId ->
                    navController.navigate("result/$runId") { launchSingleTop = true }
                }
            }
            composable(TrazoDestination.PROFILE.route) {
                profileScreen {
                    navController.navigate(SETTINGS_ROUTE) { launchSingleTop = true }
                }
            }
            composable(
                route = RESULT_ROUTE,
                arguments = listOf(navArgument("runId") { type = NavType.StringType }),
            ) {
                resultScreen {
                    navController.navigate(TrazoDestination.ACTIVITY.route) {
                        popUpTo(RESULT_ROUTE) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            composable(
                route = SAVE_RUN_ROUTE,
                arguments = listOf(navArgument("runId") { type = NavType.StringType }),
            ) {
                saveRunScreen(
                    {
                        navController.navigate(TrazoDestination.PROFILE.route) {
                            popUpTo(RESULT_ROUTE) { inclusive = true }
                            launchSingleTop = true
                        }
                        scope.launch {
                            snackbarHostState.showSnackbar(message = savedNotice, duration = SnackbarDuration.Short)
                        }
                    },
                    {
                        navController.navigate(TrazoDestination.ACTIVITY.route) {
                            popUpTo(RESULT_ROUTE) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(SETTINGS_ROUTE) {
                settingsScreen {
                    navController.popBackStack()
                }
            }
        }
    }
}
