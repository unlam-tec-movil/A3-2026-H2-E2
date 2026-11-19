package com.trazo.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomNavBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val selectedDestinations = mutableListOf<TrazoDestination>()

    private fun string(
        @StringRes id: Int,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setContent(currentRoute: String?) {
        composeTestRule.setContent {
            TrazoTheme {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onDestinationSelected = { selectedDestinations += it },
                )
            }
        }
    }

    // El ítem de NavigationBarItem es un nodo seleccionable que incluye su etiqueta; se busca
    // ese nodo (y no el texto suelto) para poder comprobar su estado de selección.
    private fun navItem(
        @StringRes labelRes: Int,
    ): SemanticsNodeInteraction = composeTestRule.onNode(hasText(string(labelRes)) and isSelectable())

    // 4.1
    @Test
    fun showsThreeDestinationLabels() {
        setContent(TrazoDestination.HOME.route)

        navItem(R.string.nav_home).assertIsDisplayed()
        navItem(R.string.nav_map).assertIsDisplayed()
        navItem(R.string.nav_history).assertIsDisplayed()
    }

    // 4.2, 4.3
    @Test
    fun homeRoute_homeIsSelected_andOthersAreNotSelectedButEnabled() {
        setContent(TrazoDestination.HOME.route)

        navItem(R.string.nav_home).assertIsSelected()
        navItem(R.string.nav_map).assertIsNotSelected().assertIsEnabled()
        navItem(R.string.nav_history).assertIsNotSelected().assertIsEnabled()
    }

    // 4.4
    @Test
    fun clickMap_notifiesMapDestination() {
        setContent(TrazoDestination.HOME.route)

        navItem(R.string.nav_map).performClick()

        assertEquals(listOf(TrazoDestination.MAP), selectedDestinations)
    }
}
