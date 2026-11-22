package com.trazo.app.ui.screens.profile

import androidx.annotation.StringRes
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.ui.components.common.StatusBadgeKind
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var settingsClicks = 0
    private val openedRunIds = mutableListOf<String>()

    private fun runCard(
        id: String,
        title: String = "Recorrido $id",
        badges: List<StatusBadgeKind> = emptyList(),
    ) = RunCardUi(
        id = id,
        title = title,
        dateText = "Mar 28 oct",
        distanceText = "5,2 km",
        durationText = "00:55:00",
        brakeCount = 3,
        regularityText = "80 %",
        badges = badges,
        accessibilityLabel = "Descripción de $title",
    )

    private fun string(
        @StringRes id: Int,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setContent(
        state: ProfileUiState,
        fontScale: Float = 1f,
    ) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                TrazoTheme {
                    ProfileContent(
                        state = state,
                        onSettingsClick = { settingsClicks++ },
                        onRunClick = { openedRunIds += it },
                    )
                }
            }
        }
    }

    /** La lista es perezosa: una card fuera de pantalla no existe hasta que se la desplaza a la vista. */
    private fun scrollToCard(id: String) {
        composeTestRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasContentDescription("Descripción de Recorrido $id"))
    }

    private fun assertTextShown(text: String) {
        composeTestRule.onNodeWithText(text).performScrollTo().assertIsDisplayed()
    }

    // 2.1, 2.3
    @Test
    fun initialState_showsEmptyRunsMessage() {
        setContent(ProfileUiState())

        assertTextShown(string(R.string.profile_empty_runs))
    }

    // 5.1, 5.2, 5.3
    @Test
    fun topBar_showsAppName_andSettingsButtonNotifiesClick() {
        setContent(ProfileUiState())

        composeTestRule.onNodeWithText(string(R.string.app_name)).assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(string(R.string.profile_settings_description))
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
            .performClick()

        assertEquals(1, settingsClicks)
    }

    @Test
    fun sectionTitles_areShownAsHeadings() {
        setContent(ProfileUiState())

        listOf(R.string.profile_stats_title, R.string.profile_runs_title).forEach { titleRes ->
            composeTestRule
                .onNodeWithText(string(titleRes))
                .performScrollTo()
                .assertIsDisplayed()
                .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        }
    }

    @Test
    fun initialState_showsZeroTotalsWithTheirLabels() {
        setContent(ProfileUiState())

        assertTextShown(string(R.string.profile_stats_runs_label))
        assertTextShown(string(R.string.profile_stats_distance_label))
        assertTextShown(string(R.string.profile_stats_time_label))
        assertTextShown("0 recorridos")
        assertTextShown("0 km")
        assertTextShown("00:00:00")
    }

    @Test
    fun oneRun_usesSingular() {
        setContent(ProfileUiState(totalRuns = 1))

        assertTextShown("1 recorrido")
    }

    @Test
    fun withTotals_showsFormattedValues() {
        setContent(ProfileUiState(totalRuns = 2, totalDistanceKm = 8.5, totalTimeSeconds = 3_300L))

        assertTextShown("2 recorridos")
        assertTextShown("8,5 km")
        assertTextShown("00:55:00")
    }

    // 2.1, 2.2: sin recorridos no hay cards
    @Test
    fun emptyState_showsNoRunCards() {
        setContent(ProfileUiState())

        assertTextShown(string(R.string.profile_empty_runs))
        composeTestRule.onNodeWithContentDescription("Descripción de Recorrido a").assertDoesNotExist()
    }

    // 2.4: el estado vacío también se comunica con texto para el lector de pantalla
    @Test
    fun emptyState_messageIsReadableText() {
        setContent(ProfileUiState())

        composeTestRule
            .onNodeWithText(string(R.string.profile_empty_runs))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
    }

    // 1.1, 1.2, 2.2: con recorridos se ven las cards, en el orden recibido, y no el estado vacío
    @Test
    fun withRuns_showsCardsInOrder_andHidesEmptyState() {
        setContent(
            ProfileUiState(
                runs = listOf(runCard("a"), runCard("b")),
                totalRuns = 2,
                totalDistanceKm = 10.4,
                totalTimeSeconds = 6_600L,
            ),
        )

        assertTextShown("2 recorridos")
        assertTextShown("10,4 km")
        composeTestRule.onNodeWithContentDescription("Descripción de Recorrido a").assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.profile_empty_runs)).assertDoesNotExist()
        scrollToCard("b")
        composeTestRule.onNodeWithContentDescription("Descripción de Recorrido b").assertIsDisplayed()
    }

    // 2.5: al llegar el primer recorrido, el estado vacío desaparece y aparece la card
    @Test
    fun firstRun_replacesEmptyStateWithCard() {
        var state by mutableStateOf(ProfileUiState())
        composeTestRule.setContent {
            TrazoTheme {
                ProfileContent(state = state, onSettingsClick = {}, onRunClick = {})
            }
        }
        assertTextShown(string(R.string.profile_empty_runs))

        state = ProfileUiState(runs = listOf(runCard("a")), totalRuns = 1)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(string(R.string.profile_empty_runs)).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Descripción de Recorrido a").assertIsDisplayed()
    }

    // 3.1 (aviso hacia afuera): tocar una card avisa con su id; no abre nada acá
    @Test
    fun tappingACard_notifiesItsId() {
        setContent(ProfileUiState(runs = listOf(runCard("a"), runCard("b"))))

        scrollToCard("b")
        composeTestRule.onNodeWithContentDescription("Descripción de Recorrido b").performClick()

        assertEquals(listOf("b"), openedRunIds)
    }

    // 6.7: fuente al 200 %
    @Test
    fun fontScale200_emptyState_isReachable() {
        setContent(ProfileUiState(), fontScale = 2f)

        assertTextShown(string(R.string.profile_stats_title))
        assertTextShown(string(R.string.profile_empty_runs))
    }

    @Test
    fun fontScale200_withRuns_cardIsReachable() {
        setContent(ProfileUiState(runs = listOf(runCard("a")), totalRuns = 1), fontScale = 2f)

        composeTestRule.onNodeWithContentDescription("Descripción de Recorrido a").performScrollTo().assertIsDisplayed()
    }

    // start-run, requisito 9: el «+» solo vive en Actividad.
    @Test
    fun doesNotShowStartRunFab() {
        setContent(ProfileUiState())

        composeTestRule
            .onNodeWithContentDescription(string(R.string.start_run_fab_description))
            .assertDoesNotExist()
    }
}
