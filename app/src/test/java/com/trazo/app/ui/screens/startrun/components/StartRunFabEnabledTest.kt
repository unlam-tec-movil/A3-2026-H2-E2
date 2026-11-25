package com.trazo.app.ui.screens.startrun.components

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class StartRunFabEnabledTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var fabClicks = 0

    private fun showFab(optionVisible: Boolean) {
        composeTestRule.setContent {
            TrazoTheme {
                StartRunFab(
                    enabled = true,
                    isOptionVisible = optionVisible,
                    onFabClick = { fabClicks++ },
                    onOptionDismiss = {},
                    onOptionClick = {},
                )
            }
        }
    }

    @Test
    fun `given an enabled fab, when tapping it, then notifies the click`() {
        // Given
        showFab(optionVisible = false)

        // When
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.start_run_fab_description))
            .assertIsEnabled()
            .performClick()

        // Then
        assertEquals(1, fabClicks)
        composeTestRule
            .onNodeWithText(context.getString(R.string.start_run_fab_option))
            .assertDoesNotExist()
    }

    @Test
    fun `given an enabled fab, when it becomes disabled, then the animation ends with the disabled fab only`() {
        // Given
        var enabled by mutableStateOf(true)
        composeTestRule.setContent {
            TrazoTheme {
                StartRunFab(
                    enabled = enabled,
                    isOptionVisible = false,
                    onFabClick = { fabClicks++ },
                    onOptionDismiss = {},
                    onOptionClick = {},
                )
            }
        }
        val description = context.getString(R.string.start_run_fab_description)
        composeTestRule.onNodeWithContentDescription(description).assertIsEnabled()

        // When
        composeTestRule.runOnIdle { enabled = false }
        composeTestRule.waitForIdle()

        // Then
        composeTestRule
            .onNodeWithContentDescription(description)
            .assertIsNotEnabled()
            .performClick()
        assertEquals(0, fabClicks)
    }
}
