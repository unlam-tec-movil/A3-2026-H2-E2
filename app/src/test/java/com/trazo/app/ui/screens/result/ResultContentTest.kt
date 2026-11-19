package com.trazo.app.ui.screens.result

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResultContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(
        @StringRes id: Int,
        vararg args: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private val goalState =
        ResultUiState(
            isLoaded = true,
            isGoalReached = true,
            goal = RunGoal.FIVE_KILOMETERS,
            distanceText = "5,2 km",
            timeText = "00:32:10",
            regularityText = "80 %",
        )

    private fun setContent(
        state: ResultUiState,
        onContinue: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            TrazoTheme { ResultContent(state = state, onContinue = onContinue) }
        }
    }

    @Test
    fun goalReached_showsGoalTitleCheckAndMetrics() {
        setContent(goalState)

        composeTestRule.onNodeWithText(string(R.string.result_goal_reached_title)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(string(R.string.result_goal_check_description)).assertIsDisplayed()
        composeTestRule.onNodeWithText("5,2 km").assertIsDisplayed()
        composeTestRule.onNodeWithText("00:32:10").assertIsDisplayed()
        composeTestRule.onNodeWithText("80 %").assertIsDisplayed()
    }

    @Test
    fun goalNotReached_showsFinishedTitle_incompleteBadge_andNoCheck() {
        setContent(goalState.copy(isGoalReached = false))

        composeTestRule.onNodeWithText(string(R.string.result_finished_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.result_incomplete)).assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(string(R.string.result_goal_check_description))
            .assertDoesNotExist()
    }

    @Test
    fun missingRegularity_showsUnavailable() {
        setContent(goalState.copy(regularityText = null))

        composeTestRule.onNodeWithText(string(R.string.result_unavailable)).assertIsDisplayed()
    }
}
