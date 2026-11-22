package com.trazo.app.ui.screens.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.saved.SavedRunTotals
import com.trazo.app.ui.components.common.StatusBadgeKind
import com.trazo.app.ui.screens.rundetail.RunDetailDialog
import com.trazo.app.ui.screens.rundetail.RunDetailUi
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recorrido completo de HU-09 y HU-16 con un recorrido de prueba: lista con totales, tocar la card abre el
 * detalle de ese recorrido y cerrarlo deja a Perfil como estaba. Las trazas en Actividad dependen del permiso
 * de ubicación y de los tiles reales, y se verifican a mano en el dispositivo.
 */
@RunWith(AndroidJUnit4::class)
class ProfileRunFlowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val card =
        RunCardUi(
            id = "a",
            title = "Vuelta al lago",
            dateText = "Mar 28 oct",
            distanceText = "5,2 km",
            durationText = "00:55:00",
            brakeCount = 3,
            regularityPercent = 80,
            badges = emptyList(),
        )

    /** Frase accesible que arma la card: es lo que el lector de pantalla y los tests encuentran. */
    private val cardDescription = "Vuelta al lago, Mar 28 oct. Distancia 5,2 km. Duración 00:55:00. 3 frenadas. Regularidad 80 %."

    private val detail =
        RunDetailUi(
            id = "a",
            title = "Vuelta al lago",
            averageSpeedText = "9,5 km/h",
            goal = RunGoal.FIVE_KILOMETERS,
            badges = listOf(StatusBadgeKind.INCOMPLETE),
            highlights = emptyList(),
            photoPaths = emptyList(),
            routePoints = listOf(RoutePoint(-34.60, -58.40, 5f, 2f, 0L), RoutePoint(-34.62, -58.38, 5f, 2f, 1L)),
            distanceText = "5,2 km",
            durationText = "00:55:00",
        )

    @Test
    fun list_tapCard_opensDetail_close_returnsToProfile() {
        composeTestRule.setContent {
            var openRunId by mutableStateOf<String?>(null)
            TrazoTheme {
                ProfileContent(
                    state =
                        ProfileUiState(
                            runs = listOf(card),
                            totals = SavedRunTotals(runCount = 1, distanceKm = 5.2, timeSeconds = 3_300L),
                        ),
                    onSettingsClick = {},
                    onRunClick = { openRunId = it },
                )
                if (openRunId == detail.id) RunDetailDialog(detail = detail, onDismiss = { openRunId = null })
            }
        }
        val closeDescription = context.getString(R.string.run_detail_close)

        // Lista y totales.
        composeTestRule.onNodeWithText("1 recorrido").assertIsDisplayed()
        composeTestRule.onNodeWithText("00:55:00").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.profile_empty_runs)).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(closeDescription).assertDoesNotExist()

        // Tocar la card abre el detalle de ese recorrido.
        composeTestRule.onNodeWithContentDescription(cardDescription).performClick()
        composeTestRule.onNodeWithContentDescription(closeDescription).assertIsDisplayed()
        composeTestRule.onNodeWithText("9,5 km/h").assertIsDisplayed()

        // Cerrar deja a Perfil como estaba.
        composeTestRule.onNodeWithContentDescription(closeDescription).performClick()
        composeTestRule.onNodeWithContentDescription(closeDescription).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(cardDescription).assertIsDisplayed()
        composeTestRule.onNodeWithText("1 recorrido").assertIsDisplayed()
    }
}
