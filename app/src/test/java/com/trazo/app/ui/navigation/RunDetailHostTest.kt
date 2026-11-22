package com.trazo.app.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.R
import com.trazo.app.data.repository.FakeSavedRunRepository
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.saved.SavedRun
import com.trazo.app.ui.screens.profile.ProfileScreen
import com.trazo.app.ui.screens.profile.ProfileViewModel
import com.trazo.app.ui.screens.rundetail.RunDetailViewModel
import com.trazo.app.ui.theme.TrazoTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Instant

/**
 * Perfil + modal de detalle tal como los cablea `TrazoApp` (requisitos 3.1, 3.8 y 5.8): tocar una card abre el
 * detalle de ese recorrido y cerrarlo deja a Perfil como estaba.
 */
@RunWith(AndroidJUnit4::class)
class RunDetailHostTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository = FakeSavedRunRepository()
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var detailViewModel: RunDetailViewModel

    private fun point(
        latitude: Double,
        longitude: Double,
    ) = RoutePoint(latitude, longitude, accuracyMeters = 5f, speedMetersPerSecond = 2f, monotonicTimestampMillis = 0L)

    private fun run(
        id: String,
        startedAtSeconds: Long,
        title: String,
    ) = SavedRun(
        id = id,
        title = title,
        startedAt = Instant.fromEpochSeconds(startedAtSeconds),
        goal = RunGoal.FIVE_KILOMETERS,
        goalReached = true,
        isDemo = false,
        distanceMeters = 5_200.0,
        elapsedTimeSeconds = 3_300L,
        averageSpeedKmh = 9.5,
        routePoints = listOf(point(-34.60, -58.40), point(-34.62, -58.38)),
        brakes = emptyList(),
        regularityPercent = 80,
        highlights = emptyList(),
        photoPaths = emptyList(),
    )

    @Before
    fun setUp() {
        profileViewModel = ProfileViewModel(repository)
        detailViewModel = RunDetailViewModel(repository)
    }

    private fun save(vararg runs: SavedRun) = runBlocking { runs.forEach { repository.save(it) } }

    private fun showProfileWithDetail() {
        composeTestRule.setContent {
            TrazoTheme {
                Column {
                    ProfileScreen(
                        onSettingsClick = {},
                        onRunClick = detailViewModel::open,
                        viewModel = profileViewModel,
                    )
                    RunDetailHost(viewModel = detailViewModel)
                }
            }
        }
    }

    private fun cardDescription(title: String) = hasContentDescription(substring = true, value = title)

    /** La lista es perezosa: una card fuera de pantalla no existe hasta desplazarla a la vista. */
    private fun openCard(title: String) {
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(cardDescription(title))
        composeTestRule.onNode(cardDescription(title)).performClick()
    }

    private fun closeButton() = composeTestRule.onNodeWithContentDescription(context.getString(R.string.run_detail_close))

    @Test
    fun `sin recorrido abierto no se ve el detalle`() {
        save(run("a", 100, "Vuelta al lago"))
        showProfileWithDetail()

        closeButton().assertDoesNotExist()
    }

    @Test
    fun `tocar una card abre el detalle de ese recorrido`() {
        save(run("a", 100, "Costanera"), run("b", 200, "Vuelta al lago"))
        showProfileWithDetail()

        openCard("Costanera")

        closeButton().assertIsDisplayed()
        // El detalle trae la velocidad promedio y la meta del recorrido abierto.
        composeTestRule.onNodeWithText(context.getString(R.string.run_detail_average_speed_label)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Costanera").assertIsDisplayed()
    }

    @Test
    fun `cerrar el detalle deja a Perfil como estaba`() {
        save(run("a", 100, "Costanera"), run("b", 200, "Vuelta al lago"))
        showProfileWithDetail()
        openCard("Costanera")
        closeButton().assertIsDisplayed()

        closeButton().performClick()

        closeButton().assertDoesNotExist()
        composeTestRule.onNode(cardDescription("Vuelta al lago")).assertIsDisplayed()
    }

    @Test
    fun `cerrar el detalle conserva la posicion de la lista`() {
        // Seis recorridos: el más antiguo queda abajo, fuera de pantalla hasta desplazar la lista.
        save(*(1..6).map { run("r$it", it * 100L, "Recorrido número $it") }.toTypedArray())
        showProfileWithDetail()
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(cardDescription("Recorrido número 1"))
        composeTestRule.onNode(cardDescription("Recorrido número 1")).assertIsDisplayed()

        openCard("Recorrido número 1")
        closeButton().performClick()

        // Sin volver a desplazar: la lista sigue en el mismo lugar.
        composeTestRule.onNode(cardDescription("Recorrido número 1")).assertIsDisplayed()
    }
}
