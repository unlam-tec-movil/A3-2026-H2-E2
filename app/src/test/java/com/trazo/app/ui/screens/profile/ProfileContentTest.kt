package com.trazo.app.ui.screens.profile

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
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

    private fun string(
        @StringRes id: Int,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setContent(state: ProfileUiState) {
        composeTestRule.setContent {
            TrazoTheme {
                ProfileContent(
                    state = state,
                    onSettingsClick = { settingsClicks++ },
                )
            }
        }
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

    // start-run, requisito 9: el «+» solo vive en Actividad.
    @Test
    fun doesNotShowStartRunFab() {
        setContent(ProfileUiState())

        composeTestRule
            .onNodeWithContentDescription(string(R.string.start_run_fab_description))
            .assertDoesNotExist()
    }
}
