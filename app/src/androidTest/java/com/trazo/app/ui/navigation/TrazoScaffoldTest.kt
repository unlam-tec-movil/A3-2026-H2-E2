package com.trazo.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Navegación de la app (TrazoApp) con pantallas falsas en los slots de [TrazoScaffold]. */
@RunWith(AndroidJUnit4::class)
class TrazoScaffoldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: NavHostController

    private fun string(
        @StringRes id: Int,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setContent() {
        composeTestRule.setContent {
            navController = rememberNavController()
            TrazoTheme {
                TrazoScaffold(
                    navController = navController,
                    activityScreen = { onNavigateToResult ->
                        Text(ACTIVITY_SCREEN)
                        Button(onClick = { onNavigateToResult(RUN_ID) }) { Text(OPEN_RESULT) }
                    },
                    profileScreen = { Text(PROFILE_SCREEN) },
                    resultScreen = { onLeaveToActivity ->
                        Text(RESULT_SCREEN)
                        Button(onClick = onLeaveToActivity) { Text(LEAVE_RESULT) }
                    },
                    saveRunScreen = { onSaved, onClose ->
                        // En columna: el NavHost apila el contenido en un Box y los botones se taparían.
                        Column {
                            Text(SAVE_RUN_SCREEN)
                            Button(onClick = onSaved) { Text(SAVE_RUN) }
                            Button(onClick = onClose) { Text(CLOSE_SAVE_RUN) }
                        }
                    },
                    settingsScreen = { Text(SETTINGS_SCREEN) },
                )
            }
        }
    }

    private fun navItem(
        @StringRes labelRes: Int,
    ): SemanticsNodeInteraction = composeTestRule.onNode(hasText(string(labelRes)) and isSelectable())

    private fun currentRoute(): String? = navController.currentBackStackEntry?.destination?.route

    @Test
    fun opensOnActivity_withActivitySelected() {
        setContent()

        composeTestRule.onNodeWithText(ACTIVITY_SCREEN).assertIsDisplayed()
        navItem(R.string.nav_activity).assertIsSelected()
        navItem(R.string.nav_profile).assertIsNotSelected()
        assertEquals(TrazoDestination.ACTIVITY.route, currentRoute())
    }

    @Test
    fun clickProfile_showsProfile_andSelectsIt() {
        setContent()

        navItem(R.string.nav_profile).performClick()

        composeTestRule.onNodeWithText(PROFILE_SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText(ACTIVITY_SCREEN).assertDoesNotExist()
        navItem(R.string.nav_profile).assertIsSelected()
        navItem(R.string.nav_activity).assertIsNotSelected()
        assertEquals(TrazoDestination.PROFILE.route, currentRoute())
    }

    @Test
    fun backToActivity_fromProfile_doesNotStackTabs() {
        setContent()

        navItem(R.string.nav_profile).performClick()
        navItem(R.string.nav_activity).performClick()

        composeTestRule.onNodeWithText(ACTIVITY_SCREEN).assertIsDisplayed()
        navItem(R.string.nav_activity).assertIsSelected()
        assertEquals(TrazoDestination.ACTIVITY.route, currentRoute())
        assertNull(navController.previousBackStackEntry)
    }

    @Test
    fun result_hidesBottomBar() {
        setContent()

        composeTestRule.onNodeWithText(OPEN_RESULT).performClick()

        composeTestRule.onNodeWithText(RESULT_SCREEN).assertIsDisplayed()
        navItem(R.string.nav_activity).assertDoesNotExist()
        navItem(R.string.nav_profile).assertDoesNotExist()
        assertEquals(RESULT_ROUTE, currentRoute())
        assertEquals(RUN_ID, navController.currentBackStackEntry?.arguments?.getString("runId"))
    }

    @Test
    fun leavingResult_returnsToActivity_andRemovesResultFromBackStack() {
        setContent()
        composeTestRule.onNodeWithText(OPEN_RESULT).performClick()

        composeTestRule.onNodeWithText(LEAVE_RESULT).performClick()

        composeTestRule.onNodeWithText(ACTIVITY_SCREEN).assertIsDisplayed()
        navItem(R.string.nav_activity).assertIsSelected()
        assertEquals(TrazoDestination.ACTIVITY.route, currentRoute())
        assertNull(navController.previousBackStackEntry)
    }

    private fun openSaveRun() {
        composeTestRule.onNodeWithText(OPEN_RESULT).performClick()
        composeTestRule.runOnIdle { navController.navigate("saverun/$RUN_ID") }
        composeTestRule.onNodeWithText(SAVE_RUN_SCREEN).assertIsDisplayed()
    }

    @Test
    fun savingARun_goesToProfile_removesResultAndSaveRun_andShowsSavedNotice() {
        setContent()
        openSaveRun()

        composeTestRule.onNodeWithText(SAVE_RUN).performClick()

        composeTestRule.onNodeWithText(PROFILE_SCREEN).assertIsDisplayed()
        navItem(R.string.nav_profile).assertIsSelected()
        assertEquals(TrazoDestination.PROFILE.route, currentRoute())
        val routes = navController.currentBackStack.value.mapNotNull { it.destination.route }
        assertFalse(RESULT_ROUTE in routes)
        assertFalse(SAVE_RUN_ROUTE in routes)
        composeTestRule.onNodeWithText(string(R.string.save_run_saved_notice)).assertIsDisplayed()
    }

    @Test
    fun closingSaveRun_returnsToActivity_withoutSavedNotice() {
        setContent()
        openSaveRun()

        composeTestRule.onNodeWithText(CLOSE_SAVE_RUN).performClick()

        composeTestRule.onNodeWithText(ACTIVITY_SCREEN).assertIsDisplayed()
        assertEquals(TrazoDestination.ACTIVITY.route, currentRoute())
        assertNull(navController.previousBackStackEntry)
        composeTestRule.onNodeWithText(string(R.string.save_run_saved_notice)).assertDoesNotExist()
    }

    @Test
    fun navigateToTab_fromCode_goesToActivity() {
        setContent()
        navItem(R.string.nav_profile).performClick()

        composeTestRule.runOnIdle { navController.navigateToTab(TrazoDestination.ACTIVITY) }

        composeTestRule.onNodeWithText(ACTIVITY_SCREEN).assertIsDisplayed()
        navItem(R.string.nav_activity).assertIsSelected()
    }

    private companion object {
        const val ACTIVITY_SCREEN = "pantalla Actividad"
        const val PROFILE_SCREEN = "pantalla Perfil"
        const val RESULT_SCREEN = "pantalla Resultado"
        const val SETTINGS_SCREEN = "pantalla Configuración"
        const val SAVE_RUN_SCREEN = "pantalla Guardar recorrido"
        const val SAVE_RUN = "guardar recorrido"
        const val CLOSE_SAVE_RUN = "cerrar guardar recorrido"
        const val OPEN_RESULT = "abrir resultado"
        const val LEAVE_RESULT = "salir del resultado"
        const val RUN_ID = "run-1"
    }
}
