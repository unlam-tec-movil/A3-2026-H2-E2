package com.trazo.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
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
import org.junit.Assert.assertTrue
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

    private fun leftOf(
        @StringRes labelRes: Int,
    ): Float {
        val item = navItem(labelRes)
        item.assertIsDisplayed()
        return item.fetchSemanticsNode().boundsInRoot.left
    }

    // 4.1
    @Test
    fun showsTwoDestinationLabelsInOrder() {
        setContent(TrazoDestination.ACTIVITY.route)

        val activityLeft = leftOf(R.string.nav_activity)
        val profileLeft = leftOf(R.string.nav_profile)
        composeTestRule.onAllNodes(isSelectable()).assertCountEquals(2)
        assertTrue(activityLeft < profileLeft)
        assertEquals(listOf(TrazoDestination.ACTIVITY, TrazoDestination.PROFILE), TrazoDestination.entries)
    }

    // 4.2, 4.3
    @Test
    fun activityRoute_activityIsSelected_andProfileIsNotSelectedButEnabled() {
        setContent(TrazoDestination.ACTIVITY.route)

        navItem(R.string.nav_activity).assertIsSelected()
        navItem(R.string.nav_profile).assertIsNotSelected().assertIsEnabled()
    }

    @Test
    fun profileRoute_profileIsSelected_andActivityIsNotSelected() {
        setContent(TrazoDestination.PROFILE.route)

        navItem(R.string.nav_profile).assertIsSelected()
        navItem(R.string.nav_activity).assertIsNotSelected().assertIsEnabled()
    }

    // 4.4
    @Test
    fun clickProfile_notifiesProfileDestination() {
        setContent(TrazoDestination.ACTIVITY.route)

        navItem(R.string.nav_profile).performClick()

        assertEquals(listOf(TrazoDestination.PROFILE), selectedDestinations)
    }

    @Test
    fun clickActivity_notifiesActivityDestination() {
        setContent(TrazoDestination.PROFILE.route)

        navItem(R.string.nav_activity).performClick()

        assertEquals(listOf(TrazoDestination.ACTIVITY), selectedDestinations)
    }
}
