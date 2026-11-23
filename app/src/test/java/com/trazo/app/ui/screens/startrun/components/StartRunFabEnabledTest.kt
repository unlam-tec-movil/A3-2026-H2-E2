package com.trazo.app.ui.screens.startrun.components

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
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
        composeTestRule.onNodeWithText(context.getString(R.string.start_run_fab_option)).assertDoesNotExist()
    }
}
