package com.trazo.app.ui.screens.map.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.domain.model.stops.StopKind
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StopClassificationDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val clicks = mutableListOf<StopKind>()

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setContent(fontScale: Float = 1f) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                TrazoTheme { StopClassificationDialog(onClassify = { clicks += it }) }
            }
        }
    }

    @Test
    fun fontScale200_buttonsVisible_andAtLeast48dp() {
        setContent(fontScale = 2f)

        listOf(R.string.map_stop_query_brake, R.string.map_stop_query_personal_pause).forEach { id ->
            composeTestRule
                .onNodeWithText(string(id))
                .assertIsDisplayed()
                .assertHeightIsAtLeast(Sizes.minTouchTarget)
        }
    }

    @Test
    fun fontScale200_questionTextVisible() {
        setContent(fontScale = 2f)

        composeTestRule.onNodeWithText(string(R.string.map_stop_query_title)).assertIsDisplayed()
    }

    @Test
    fun clickBrake_deliversBrake() {
        setContent()

        composeTestRule.onNodeWithText(string(R.string.map_stop_query_brake)).performClick()

        assertEquals(listOf(StopKind.BRAKE), clicks)
    }

    @Test
    fun clickPersonalPause_deliversPersonalPause() {
        setContent()

        composeTestRule.onNodeWithText(string(R.string.map_stop_query_personal_pause)).performClick()

        assertEquals(listOf(StopKind.PERSONAL_PAUSE), clicks)
    }
}
