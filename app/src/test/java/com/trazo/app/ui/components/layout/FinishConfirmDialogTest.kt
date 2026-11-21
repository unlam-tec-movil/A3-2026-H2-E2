package com.trazo.app.ui.components.layout

import android.content.Context
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.R
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinishConfirmDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var confirmations = 0
    private var cancellations = 0

    private fun showDialog(
        visible: Boolean,
        goalReached: Boolean,
        finishFailed: Boolean = false,
        isFinishing: Boolean = false,
    ) {
        composeTestRule.setContent {
            TrazoTheme {
                FinishConfirmDialog(
                    showFinishConfirmDialog = visible,
                    goalReached = goalReached,
                    onConfirmFinish = { confirmations++ },
                    onCancelFinish = { cancellations++ },
                    finishFailed = finishFailed,
                    isFinishing = isFinishing,
                )
            }
        }
    }

    @Test
    fun `given the dialog is hidden, when it is composed, then nothing is displayed`() {
        // Given / When
        showDialog(visible = false, goalReached = true)

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.map_finish)).assertDoesNotExist()
    }

    @Test
    fun `given the goal was reached, when the dialog is shown, then shows the goal reached message`() {
        // Given / When
        showDialog(visible = true, goalReached = true)

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.map_finish_confirm_goal_reached)).assertExists()
    }

    @Test
    fun `given the goal was not reached, when the dialog is shown, then shows the not reached message`() {
        // Given / When
        showDialog(visible = true, goalReached = false)

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.map_finish_confirm_goal_not_reached)).assertExists()
    }

    @Test
    fun `given the dialog is shown, when confirming, then notifies the confirmation`() {
        // Given
        showDialog(visible = true, goalReached = true)

        // When
        composeTestRule.onNodeWithText(context.getString(R.string.map_finish)).performClick()

        // Then
        assertEquals(1, confirmations)
        assertEquals(0, cancellations)
    }

    @Test
    fun `given the dialog is shown, when cancelling, then notifies the cancellation`() {
        // Given
        showDialog(visible = true, goalReached = true)

        // When
        composeTestRule.onNodeWithText(context.getString(R.string.map_cancel)).performClick()

        // Then
        assertEquals(1, cancellations)
        assertEquals(0, confirmations)
    }

    @Test
    fun `given the finish failed, when the dialog is shown, then shows the error and the retry button`() {
        // Given / When
        showDialog(visible = true, goalReached = true, finishFailed = true)

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.map_finish_error)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.map_retry)).performClick()
        assertEquals(1, confirmations)
    }

    @Test
    fun `given the finish is in progress, when the dialog is shown, then both buttons are disabled`() {
        // Given / When
        showDialog(visible = true, goalReached = true, isFinishing = true)

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.map_finish)).assertIsNotEnabled()
        composeTestRule.onNodeWithText(context.getString(R.string.map_cancel)).assertIsNotEnabled()
    }
}
