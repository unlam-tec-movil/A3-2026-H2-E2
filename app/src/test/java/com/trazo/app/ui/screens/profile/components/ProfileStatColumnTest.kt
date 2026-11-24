package com.trazo.app.ui.screens.profile.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
class ProfileStatColumnTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysLabelAndValue() {
        composeTestRule.setContent {
            TrazoTheme {
                ProfileStatColumn(
                    label = "Recorridos guardados",
                    value = "5",
                    fill = 0.25f,
                    barColor = Color.Blue,
                )
            }
        }

        composeTestRule.onNodeWithText("5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recorridos guardados").assertIsDisplayed()
    }

    @Test
    fun rendersWithZeroFill() {
        composeTestRule.setContent {
            TrazoTheme {
                ProfileStatColumn(
                    label = "Distancia total",
                    value = "0,0 km",
                    fill = 0f,
                    barColor = Color.Green,
                )
            }
        }

        composeTestRule.onNodeWithText("0,0 km").assertIsDisplayed()
        composeTestRule.onNodeWithText("Distancia total").assertIsDisplayed()
    }

    @Test
    fun rendersWithFullFill() {
        composeTestRule.setContent {
            TrazoTheme {
                ProfileStatColumn(
                    label = "Tiempo total",
                    value = "10:00:00",
                    fill = 1f,
                    barColor = Color.Yellow,
                )
            }
        }

        composeTestRule.onNodeWithText("10:00:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tiempo total").assertIsDisplayed()
    }

    @Test
    fun modifier_isApplied() {
        composeTestRule.setContent {
            TrazoTheme {
                ProfileStatColumn(
                    label = "Métrica",
                    value = "10",
                    fill = 0.5f,
                    barColor = Color.Red,
                    modifier = Modifier.testTag("stat_column"),
                )
            }
        }

        composeTestRule.onNodeWithTag("stat_column").assertIsDisplayed()
    }
}
