package com.trazo.app.ui.screens.rundetail

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
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
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.ui.components.common.StatusBadgeKind
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Modal de detalle en dispositivo: fuente al 200 % y descripciones accesibles (requisitos 6.3, 6.6 y 6.7). */
@RunWith(AndroidJUnit4::class)
class RunDetailDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var dismissals = 0

    private fun point(
        latitude: Double,
        longitude: Double,
    ) = RoutePoint(latitude, longitude, 5f, 2f, 0L)

    private val detail =
        RunDetailUi(
            id = "a",
            title = "Vuelta al lago",
            averageSpeedText = "9,5 km/h",
            goal = RunGoal.TEN_KILOMETERS,
            badges = listOf(StatusBadgeKind.GOAL_REACHED, StatusBadgeKind.DEMO),
            highlights = listOf("Ritmo parejo"),
            photoPaths = emptyList(),
            routePoints = listOf(point(-34.60, -58.40), point(-34.62, -58.38), point(-34.64, -58.36)),
            distanceText = "10,3 km",
            durationText = "00:55:00",
        )

    private fun show(
        detail: RunDetailUi,
        fontScale: Float = 1f,
    ) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                TrazoTheme { RunDetailDialog(detail = detail, onDismiss = { dismissals++ }) }
            }
        }
    }

    @Test
    fun dialog_showsTheDetailWithTheMapDescribedForScreenReaders() {
        show(detail)

        composeTestRule.onNodeWithText("Vuelta al lago").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Mapa con la traza del recorrido Vuelta al lago. Distancia 10,3 km, duración 00:55:00.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("9,5 km/h").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.profile_run_badge_demo)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun dialog_withoutADrawableTrace_showsTheAlternativeTextAndTheRest() {
        show(detail.copy(routePoints = listOf(point(-34.60, -58.40))))

        composeTestRule.onNodeWithText(context.getString(R.string.run_detail_map_unavailable)).assertIsDisplayed()
        composeTestRule.onNodeWithText("9,5 km/h").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun dialog_closeButtonIsAtLeast48dp_andNotifies() {
        show(detail)

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.run_detail_close))
            .assertWidthIsAtLeast(Sizes.minTouchTarget)
            .assertHeightIsAtLeast(Sizes.minTouchTarget)
            .performClick()

        assertEquals(1, dismissals)
    }

    @Test
    fun dialog_withFontScale200_contentIsReachableAndCloseWorks() {
        show(detail, fontScale = 2f)

        composeTestRule.onNodeWithText("Vuelta al lago").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ritmo parejo").performScrollTo().assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.run_detail_close))
            .assertHeightIsAtLeast(Sizes.minTouchTarget)
            .performClick()

        assertEquals(1, dismissals)
    }
}
