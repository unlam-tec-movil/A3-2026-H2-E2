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
class MotionSensorMissingNoticeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test
    fun notice_showsTitleDetailAndIcon() {
        composeTestRule.setContent { TrazoTheme { MotionSensorMissingNotice() } }

        composeTestRule.onNodeWithText(string(R.string.map_motion_sensor_missing_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.map_motion_sensor_missing_detail)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(MOTION_SENSOR_MISSING_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun notice_isPoliteLiveRegion() {
        composeTestRule.setContent { TrazoTheme { MotionSensorMissingNotice() } }

        composeTestRule
            .onNode(hasText(string(R.string.map_motion_sensor_missing_title)))
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
            )
    }
}
