package com.trazo.app.ui.components.common

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.R
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartRunDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val selectedGoals = mutableListOf<RunGoal>()
    private var confirmations = 0
    private var dismissals = 0

    private fun showDialog(selectedGoal: RunGoal?) {
        composeTestRule.setContent {
            TrazoTheme {
                StartRunDialog(
                    selectedGoal = selectedGoal,
                    onGoalSelected = { selectedGoals.add(it) },
                    onConfirm = { confirmations++ },
                    onDismiss = { dismissals++ },
                )
            }
        }
    }

    @Test
    fun `given no selected goal, when the dialog is shown, then the hint is visible and confirm is disabled`() {
        // Given / When
        showDialog(selectedGoal = null)

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.start_run_hint)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.start_run_confirm)).assertIsNotEnabled()
    }

    @Test
    fun `given a selected goal, when the dialog is shown, then the hint is hidden and confirm is enabled`() {
        // Given / When
        showDialog(selectedGoal = RunGoal.TEN_KILOMETERS)

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.start_run_hint)).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.start_run_confirm)).assertIsEnabled()
    }

    @Test
    fun `given the dialog, when tapping a goal chip, then notifies the selected goal`() {
        // Given
        showDialog(selectedGoal = null)

        // When
        composeTestRule.onNodeWithText(context.getString(R.string.start_run_goal_km, 5)).performClick()

        // Then
        assertEquals(listOf(RunGoal.FIVE_KILOMETERS), selectedGoals)
    }

    @Test
    fun `given a selected goal, when confirming, then notifies the confirmation`() {
        // Given
        showDialog(selectedGoal = RunGoal.THREE_KILOMETERS)

        // When
        composeTestRule.onNodeWithText(context.getString(R.string.start_run_confirm)).performClick()

        // Then
        assertEquals(1, confirmations)
    }

    @Test
    fun `given the dialog, when cancelling, then notifies the dismissal`() {
        // Given
        showDialog(selectedGoal = null)

        // When
        composeTestRule.onNodeWithText(context.getString(R.string.start_run_cancel)).performClick()

        // Then
        assertEquals(1, dismissals)
    }
}
