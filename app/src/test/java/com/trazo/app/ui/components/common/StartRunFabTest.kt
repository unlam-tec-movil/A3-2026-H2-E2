package com.trazo.app.ui.components.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Casos del «+» común que no cubre `ProfileContentTest` (start-run, requisito 9). */
@RunWith(AndroidJUnit4::class)
class StartRunFabTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // 9.5
    @Test
    fun disabled_isNotEnabled_doesNotNotify_andHidesTheOption() {
        var fabClicks = 0
        composeTestRule.setContent {
            TrazoTheme {
                StartRunFab(
                    enabled = false,
                    isOptionVisible = true,
                    onFabClick = { fabClicks++ },
                    onOptionDismiss = {},
                    onOptionClick = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.start_run_fab_description))
            .assertIsDisplayed()
            .assertIsNotEnabled()
            .performClick()

        assertEquals(0, fabClicks)
        composeTestRule.onNodeWithText(context.getString(R.string.start_run_fab_option)).assertDoesNotExist()
    }
}
