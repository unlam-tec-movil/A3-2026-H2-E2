package com.trazo.app.ui.screens.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.data.repository.FakeSavedRunRepository
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenWrapperTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `given the profile view model, when the profile screen is shown, then it renders its state`() {
        // Given
        val viewModel = ProfileViewModel(FakeSavedRunRepository())

        // When
        composeTestRule.setContent {
            TrazoTheme {
                ProfileScreen(
                    onSettingsClick = {},
                    viewModel = viewModel,
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.profile_stats_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.profile_empty_runs)).performScrollTo().assertIsDisplayed()
    }
}
