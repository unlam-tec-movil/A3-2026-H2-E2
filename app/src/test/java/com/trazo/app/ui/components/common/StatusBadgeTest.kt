package com.trazo.app.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Marcas de estado: el estado se comunica con texto, no solo con color (requisitos 1.5, 1.6, 3.7, 6.4). */
@RunWith(AndroidJUnit4::class)
class StatusBadgeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun show(kind: StatusBadgeKind) {
        composeTestRule.setContent { TrazoTheme { StatusBadge(kind = kind) } }
    }

    @Test
    fun incomplete_showsItsText() {
        show(StatusBadgeKind.INCOMPLETE)

        composeTestRule.onNodeWithText(context.getString(R.string.profile_run_badge_incomplete)).assertIsDisplayed()
    }

    @Test
    fun demo_showsItsText() {
        show(StatusBadgeKind.DEMO)

        composeTestRule.onNodeWithText(context.getString(R.string.profile_run_badge_demo)).assertIsDisplayed()
    }

    @Test
    fun goalReached_showsItsText() {
        show(StatusBadgeKind.GOAL_REACHED)

        composeTestRule.onNodeWithText(context.getString(R.string.profile_run_badge_goal_reached)).assertIsDisplayed()
    }

    @Test
    fun severalBadges_eachKeepsItsOwnText() {
        composeTestRule.setContent {
            TrazoTheme {
                Column {
                    StatusBadge(kind = StatusBadgeKind.INCOMPLETE)
                    StatusBadge(kind = StatusBadgeKind.DEMO)
                }
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.profile_run_badge_incomplete)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.profile_run_badge_demo)).assertIsDisplayed()
    }
}
