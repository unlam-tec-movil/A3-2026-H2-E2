package com.trazo.app.ui.screens.profile.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmptyStateTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysTitleAndBody() {
        composeTestRule.setContent {
            TrazoTheme {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    title = "Sin recorridos todavía",
                    body = "Salí a pedalear para ver tus estadísticas.",
                )
            }
        }

        composeTestRule.onNodeWithText("Sin recorridos todavía").assertIsDisplayed()
        composeTestRule.onNodeWithText("Salí a pedalear para ver tus estadísticas.").assertIsDisplayed()
    }

    @Test
    fun appliesModifier() {
        composeTestRule.setContent {
            TrazoTheme {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    title = "Título",
                    body = "Detalle",
                    modifier = Modifier.testTag("empty_state_tag"),
                )
            }
        }

        composeTestRule.onNodeWithTag("empty_state_tag").assertIsDisplayed()
    }
}
