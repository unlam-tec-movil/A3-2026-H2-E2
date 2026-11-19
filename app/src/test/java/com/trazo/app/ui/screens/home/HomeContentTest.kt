package com.trazo.app.ui.screens.home

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.ui.components.common.StartRunFab
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var settingsClicks = 0
    private var fabClicks = 0
    private var optionDismissals = 0
    private var optionClicks = 0

    private fun string(
        @StringRes id: Int,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    /** El «+» llega como slot desde `TrazoApp`; acá se arma con contadores. */
    private fun setContent(
        state: HomeUiState,
        isOptionVisible: Boolean = false,
    ) {
        composeTestRule.setContent {
            TrazoTheme {
                HomeContent(
                    state = state,
                    onSettingsClick = { settingsClicks++ },
                    startRunFab = { modifier ->
                        StartRunFab(
                            enabled = true,
                            isOptionVisible = isOptionVisible,
                            onFabClick = { fabClicks++ },
                            onOptionDismiss = { optionDismissals++ },
                            onOptionClick = { optionClicks++ },
                            modifier = modifier,
                        )
                    },
                )
            }
        }
    }

    // 2.1, 2.3
    @Test
    fun initialState_showsEmptyRunsMessage() {
        setContent(HomeUiState())

        composeTestRule.onNodeWithText(string(R.string.home_empty_runs)).assertIsDisplayed()
    }

    // 5.1, 5.2, 5.3
    @Test
    fun topBar_showsAppName_andSettingsButtonNotifiesClick() {
        setContent(HomeUiState())

        composeTestRule.onNodeWithText(string(R.string.app_name)).assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(string(R.string.home_settings_description))
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
            .performClick()

        assertEquals(1, settingsClicks)
        assertEquals(0, fabClicks)
    }

    // 3.1, 3.2, 3.5
    @Test
    fun startRunFab_isEnabledAndClickable_andNotifiesClick() {
        setContent(HomeUiState())

        composeTestRule
            .onNodeWithContentDescription(string(R.string.home_start_run_fab_description))
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
            .performClick()

        assertEquals(1, fabClicks)
    }
}
