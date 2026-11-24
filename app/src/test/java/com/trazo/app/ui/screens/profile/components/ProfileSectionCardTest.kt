package com.trazo.app.ui.screens.profile.components

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
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
class ProfileSectionCardTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun title_isDisplayedWithHeadingSemantics() {
        composeTestRule.setContent {
            TrazoTheme {
                ProfileSectionCard(title = "Mis estadísticas") {
                    Text(text = "Contenido")
                }
            }
        }

        composeTestRule
            .onNodeWithText("Mis estadísticas")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun content_isRenderedInsideCard() {
        composeTestRule.setContent {
            TrazoTheme {
                ProfileSectionCard(title = "Sección") {
                    Text(text = "Texto interno", modifier = Modifier.testTag("inner_text"))
                }
            }
        }

        composeTestRule.onNodeWithTag("inner_text").assertIsDisplayed()
    }

    @Test
    fun modifier_isAppliedToCard() {
        composeTestRule.setContent {
            TrazoTheme {
                ProfileSectionCard(
                    title = "Sección",
                    modifier = Modifier.testTag("card_tag"),
                ) {
                    Text(text = "Contenido")
                }
            }
        }

        composeTestRule.onNodeWithTag("card_tag").assertIsDisplayed()
    }
}
