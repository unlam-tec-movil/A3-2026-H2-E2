package com.trazo.app.ui.screens.rundetail

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.ui.components.common.StatusBadgeKind
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Contenido del modal de detalle (requisitos 3.2 a 3.9, 6.3 y 6.7). */
@RunWith(AndroidJUnit4::class)
class RunDetailContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var dismissals = 0

    private fun point(
        latitude: Double,
        longitude: Double,
    ) = RoutePoint(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = 5f,
        speedMetersPerSecond = 2f,
        monotonicTimestampMillis = 0L,
    )

    private val trace = listOf(point(-34.60, -58.40), point(-34.62, -58.38), point(-34.64, -58.36))

    private fun detail(
        title: String = "Vuelta al lago",
        statusBadge: StatusBadgeKind = StatusBadgeKind.GOAL_REACHED,
        isDemo: Boolean = false,
        highlights: List<String> = emptyList(),
        photoPaths: List<String> = emptyList(),
        routePoints: List<RoutePoint> = trace,
    ) = RunDetailUi(
        id = "a",
        title = title,
        averageSpeedText = "9,5 km/h",
        goalText = "Meta 10 km",
        statusBadge = statusBadge,
        isDemo = isDemo,
        highlights = highlights,
        photoPaths = photoPaths,
        routePoints = routePoints,
        mapDescription = "Mapa con la traza del recorrido $title.",
    )

    private fun show(
        detail: RunDetailUi,
        fontScale: Float = 1f,
    ) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                TrazoTheme { RunDetailContent(detail = detail, onDismiss = { dismissals++ }) }
            }
        }
    }

    @Test
    fun `muestra titulo velocidad promedio meta y estado`() {
        show(detail(title = "Vuelta al lago", statusBadge = StatusBadgeKind.GOAL_REACHED))

        composeTestRule.onNodeWithText("Vuelta al lago").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.run_detail_average_speed_label)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("9,5 km/h").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Meta 10 km").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.profile_run_badge_goal_reached)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `un recorrido incompleto muestra la marca Incompleto`() {
        show(detail(statusBadge = StatusBadgeKind.INCOMPLETE))

        composeTestRule.onNodeWithText(context.getString(R.string.profile_run_badge_incomplete)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `un recorrido demo muestra la marca Demo`() {
        show(detail(isDemo = true))

        composeTestRule.onNodeWithText(context.getString(R.string.profile_run_badge_demo)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `un recorrido real no muestra la marca Demo`() {
        show(detail(isDemo = false))

        composeTestRule.onNodeWithText(context.getString(R.string.profile_run_badge_demo)).assertDoesNotExist()
    }

    @Test
    fun `sin destacados no hay seccion de destacados`() {
        show(detail(highlights = emptyList()))

        composeTestRule.onNodeWithText(context.getString(R.string.run_detail_highlights_title)).assertDoesNotExist()
    }

    @Test
    fun `con destacados los muestra`() {
        show(detail(highlights = listOf("Ritmo parejo")))

        composeTestRule.onNodeWithText(context.getString(R.string.run_detail_highlights_title)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Ritmo parejo").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `sin fotos no hay seccion de fotos`() {
        show(detail(photoPaths = emptyList()))

        composeTestRule.onNodeWithText(context.getString(R.string.run_detail_photos_title)).assertDoesNotExist()
    }

    @Test
    fun `con fotos muestra la seccion`() {
        show(detail(photoPaths = listOf("/foto-1.jpg")))

        composeTestRule.onNodeWithText(context.getString(R.string.run_detail_photos_title)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `si la traza no se puede mostrar el resto del detalle sigue visible`() {
        show(detail(routePoints = listOf(point(-34.60, -58.40)), highlights = listOf("Ritmo parejo")))

        composeTestRule.onNodeWithText(context.getString(R.string.run_detail_map_unavailable)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Vuelta al lago").assertIsDisplayed()
        composeTestRule.onNodeWithText("9,5 km/h").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Ritmo parejo").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `con traza dibujable el mapa tiene su descripcion accesible`() {
        show(detail(title = "Vuelta al lago"))

        composeTestRule.onNodeWithContentDescription("Mapa con la traza del recorrido Vuelta al lago.").assertIsDisplayed()
    }

    @Test
    fun `el boton Cerrar avisa y mide al menos 48 dp`() {
        show(detail())

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.run_detail_close))
            .assertHasClickAction()
            .assertWidthIsAtLeast(Sizes.minTouchTarget)
            .assertHeightIsAtLeast(Sizes.minTouchTarget)
            .performClick()

        assertEquals(1, dismissals)
    }

    @Test
    fun `con la fuente al 200 por ciento el cierre y el contenido siguen operables`() {
        show(detail(highlights = listOf("Ritmo parejo")), fontScale = 2f)

        composeTestRule.onNodeWithText("Vuelta al lago").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.run_detail_close))
            .assertHeightIsAtLeast(Sizes.minTouchTarget)
            .performClick()
        composeTestRule.onNodeWithText("Ritmo parejo").performScrollTo().assertIsDisplayed()

        assertEquals(1, dismissals)
    }
}
