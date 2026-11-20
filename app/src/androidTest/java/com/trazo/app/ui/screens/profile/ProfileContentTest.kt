package com.trazo.app.ui.screens.profile

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var settingsClicks = 0

    private fun string(
        @StringRes id: Int,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setContent(state: ProfileUiState) {
        composeTestRule.setContent {
            TrazoTheme {
                ProfileContent(
                    state = state,
                    onSettingsClick = { settingsClicks++ },
                )
            }
        }
    }

    // 2.1, 2.3
    @Test
    fun initialState_showsEmptyRunsMessage() {
        setContent(ProfileUiState())

        composeTestRule.onNodeWithText(string(R.string.profile_empty_runs)).performScrollTo().assertIsDisplayed()
    }

    // 5.1, 5.2, 5.3
    @Test
    fun topBar_showsAppName_andSettingsButtonNotifiesClick() {
        setContent(ProfileUiState())

        composeTestRule.onNodeWithText(string(R.string.app_name)).assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(string(R.string.profile_settings_description))
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
            .performClick()

        assertEquals(1, settingsClicks)
    }
}
