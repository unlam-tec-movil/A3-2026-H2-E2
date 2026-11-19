package com.trazo.app.ui.screens.home

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenWrapperTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `given the home view model, when the home screen is shown, then it renders the start run slot`() {
        // Given
        val viewModel = HomeViewModel()

        // When
        composeTestRule.setContent {
            TrazoTheme {
                HomeScreen(
                    startRunFab = { Text("start-run-fab") },
                    onSettingsClick = {},
                    viewModel = viewModel,
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("start-run-fab").assertIsDisplayed()
    }
}
