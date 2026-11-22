package com.trazo.app.ui.screens.profile.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.ui.components.common.StatusBadgeKind
import com.trazo.app.ui.screens.profile.RunCardUi
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Card de «Mis recorridos» (requisitos 1.3 a 1.6, 3.1, 5.8, 6.3 a 6.5, 6.7). */
@RunWith(AndroidJUnit4::class)
class RunCardTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var clicks = 0

    private fun card(
        title: String = "Vuelta al lago",
        badges: List<StatusBadgeKind> = emptyList(),
        brakeCount: Int = 7,
        regularityPercent: Int? = 80,
    ) = RunCardUi(
        id = "run-1",
        title = title,
        dateText = "Mar 28 oct",
        distanceText = "5,2 km",
        durationText = "00:55:00",
        brakeCount = brakeCount,
        regularityPercent = regularityPercent,
        badges = badges,
    )

    /** Frase que la card arma sola con los datos de [card]; el lector de pantalla la lee como un botón. */
    private val defaultLabel = "Vuelta al lago, Mar 28 oct. Distancia 5,2 km. Duración 00:55:00. 7 frenadas. Regularidad 80 %."

    private fun show(
        run: RunCardUi,
        fontScale: Float = 1f,
    ) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                TrazoTheme { RunCard(run = run, onClick = { clicks++ }) }
            }
        }
    }

    private fun assertShown(text: String) {
        // Las partes de la card se leen juntas: se buscan en el árbol sin fusionar.
        composeTestRule.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `muestra titulo fecha distancia duracion frenadas y regularidad`() {
        show(card())

        listOf("Vuelta al lago", "Mar 28 oct", "5,2 km", "00:55:00", "7", "80 %").forEach(::assertShown)
        listOf(
            R.string.profile_run_distance_label,
            R.string.profile_run_time_label,
            R.string.profile_run_brakes_label,
            R.string.profile_run_regularity_label,
        ).forEach { assertShown(context.getString(it)) }
    }

    @Test
    fun `un titulo largo ocupa una sola linea con puntos suspensivos`() {
        show(card(title = "Mi recorrido larguisimo de domingo por la costanera hasta el puerto y vuelta por el parque"))

        val results = mutableListOf<TextLayoutResult>()
        composeTestRule
            .onNodeWithText("Mi recorrido larguisimo", substring = true, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action!!
            .invoke(results)

        assertEquals(1, results.single().lineCount)
        assertTrue("El título debería cortarse con puntos suspensivos", results.single().hasVisualOverflow)
    }

    @Test
    fun `las marcas se muestran con su texto`() {
        show(card(badges = listOf(StatusBadgeKind.INCOMPLETE, StatusBadgeKind.DEMO)))

        assertShown(context.getString(R.string.profile_run_badge_incomplete))
        assertShown(context.getString(R.string.profile_run_badge_demo))
    }

    @Test
    fun `sin marcas no aparece ninguna`() {
        show(card(badges = emptyList()))

        composeTestRule
            .onNodeWithText(context.getString(R.string.profile_run_badge_incomplete), useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(context.getString(R.string.profile_run_badge_demo), useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `toda la card se lee como una sola descripcion accesible completa`() {
        show(card(badges = listOf(StatusBadgeKind.INCOMPLETE, StatusBadgeKind.DEMO)))

        composeTestRule
            .onNodeWithContentDescription("$defaultLabel Marcas: Incompleto, Demo.")
            .assertIsDisplayed()
    }

    @Test
    fun `sin marcas la descripcion accesible no menciona marcas`() {
        show(card(brakeCount = 1))

        val label = "Vuelta al lago, Mar 28 oct. Distancia 5,2 km. Duración 00:55:00. 1 frenada. Regularidad 80 %."
        composeTestRule.onNodeWithContentDescription(label).assertIsDisplayed()
    }

    @Test
    fun `sin dato de regularidad la card muestra un guion y la descripcion dice sin datos`() {
        show(card(regularityPercent = null))

        assertShown("–")
        val label = "Vuelta al lago, Mar 28 oct. Distancia 5,2 km. Duración 00:55:00. 7 frenadas. Regularidad sin datos."
        composeTestRule.onNodeWithContentDescription(label).assertIsDisplayed()
    }

    @Test
    fun `tocar la card avisa y su area tactil es de al menos 48 dp`() {
        show(card())

        composeTestRule
            .onNodeWithContentDescription(defaultLabel)
            .assertHasClickAction()
            .assertHeightIsAtLeast(Sizes.minTouchTarget)
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `con la fuente al 200 por ciento todo sigue visible`() {
        show(card(badges = listOf(StatusBadgeKind.INCOMPLETE, StatusBadgeKind.DEMO)), fontScale = 2f)

        listOf("Vuelta al lago", "Mar 28 oct", "5,2 km", "00:55:00", "7", "80 %").forEach(::assertShown)
        assertShown(context.getString(R.string.profile_run_regularity_label))
        assertShown(context.getString(R.string.profile_run_badge_incomplete))
        assertShown(context.getString(R.string.profile_run_badge_demo))
    }
}
