package com.trazo.app.ui.theme

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun extendedColors_defaultValuesAreConfigured() {
        val colors = TrazoExtendedColors()
        assertEquals(Neutral300, colors.statBarRuns)
        assertEquals(Neutral500, colors.statBarDistance)
        assertEquals(Neutral600, colors.statBarTime)
    }

    @Test
    fun trazoTheme_providesExtendedColorsInComposition() {
        var colorsInComposition: TrazoExtendedColors? = null
        composeTestRule.setContent {
            TrazoTheme {
                colorsInComposition = TrazoTheme.extendedColors
            }
        }
        assertNotNull(colorsInComposition)
        assertEquals(Neutral300, colorsInComposition?.statBarRuns)
        assertEquals(Neutral500, colorsInComposition?.statBarDistance)
        assertEquals(Neutral600, colorsInComposition?.statBarTime)
    }
}
