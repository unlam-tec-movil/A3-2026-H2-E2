package com.trazo.app.ui.screens.rundetail.components

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Destacados del detalle: chips de solo lectura (requisitos 3.3 y 3.4). */
@RunWith(AndroidJUnit4::class)
class HighlightChipsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun show(highlights: List<String>) {
        composeTestRule.setContent {
            TrazoTheme {
                Box(modifier = Modifier.testTag("host")) { HighlightChips(highlights = highlights) }
            }
        }
    }

    @Test
    fun `muestra el titulo de la seccion y cada destacado`() {
        show(listOf("Ritmo parejo", "Sin frenadas largas", "Mejor marca del mes"))

        composeTestRule.onNodeWithText(context.getString(R.string.run_detail_highlights_title)).assertIsDisplayed()
        listOf("Ritmo parejo", "Sin frenadas largas", "Mejor marca del mes").forEach {
            composeTestRule.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test
    fun `sin destacados no ocupa espacio ni muestra el titulo`() {
        show(emptyList())

        composeTestRule.onNodeWithTag("host").assertHeightIsEqualTo(0.dp)
        composeTestRule.onNodeWithText(context.getString(R.string.run_detail_highlights_title)).assertDoesNotExist()
    }

    @Test
    fun `un destacado muy largo sigue visible`() {
        val long = "Un destacado larguísimo que no entra en una sola línea del ancho de la pantalla del teléfono"
        show(listOf(long))

        composeTestRule.onNodeWithText(long).assertIsDisplayed()
    }
}
