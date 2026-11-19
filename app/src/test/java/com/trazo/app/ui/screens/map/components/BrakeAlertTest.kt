package com.trazo.app.ui.screens.map.components

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrakeAlertTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test
    fun visibleAlert_showsTitleDetailAndIcon() {
        composeTestRule.setContent { TrazoTheme { BrakeAlert(visible = true) } }

        composeTestRule.onNodeWithText(string(R.string.map_brake_alert_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.map_brake_alert_detail)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BRAKE_ALERT_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun visibleAlert_isPoliteLiveRegion() {
        composeTestRule.setContent { TrazoTheme { BrakeAlert(visible = true) } }

        composeTestRule
            .onNode(hasText(string(R.string.map_brake_alert_title)))
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
            )
    }

    @Test
    fun hiddenAlert_doesNotExist() {
        composeTestRule.setContent { TrazoTheme { BrakeAlert(visible = false) } }

        composeTestRule.onNodeWithText(string(R.string.map_brake_alert_title)).assertDoesNotExist()
    }
}
