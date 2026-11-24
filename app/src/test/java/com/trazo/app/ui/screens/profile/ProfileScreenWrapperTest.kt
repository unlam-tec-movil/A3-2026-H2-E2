package com.trazo.app.ui.screens.profile

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.data.repository.FakeSavedRunRepository
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.saved.SavedRun
import com.trazo.app.ui.theme.TrazoTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class ProfileScreenWrapperTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `given the profile view model, when the profile screen is shown, then it renders its state`() {
        val viewModel = ProfileViewModel(FakeSavedRunRepository())

        composeTestRule.setContent {
            TrazoTheme {
                ProfileScreen(
                    onSettingsClick = {},
                    viewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.profile_stats_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.profile_empty_runs)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `given profile with saved runs, when shown, then renders runs section and notifies run clicks`() =
        runBlocking {
            val repository = FakeSavedRunRepository()
            repository.save(
                SavedRun(
                    id = "run-101",
                    title = "Recorrido Parque",
                    startedAt = Instant.fromEpochSeconds(1_700_000_000L),
                    goal = RunGoal.FIVE_KILOMETERS,
                    goalReached = true,
                    isDemo = false,
                    distanceMeters = 5_000.0,
                    elapsedTimeSeconds = 1_800L,
                    averageSpeedKmh = 10.0,
                    routePoints = emptyList(),
                    brakes = emptyList(),
                    regularityPercent = 90,
                    highlights = emptyList(),
                    photoPaths = emptyList(),
                ),
            )
            val viewModel = ProfileViewModel(repository)
            var clickedRunId: String? = null

            composeTestRule.setContent {
                TrazoTheme {
                    ProfileScreen(
                        onSettingsClick = {},
                        onRunClick = { clickedRunId = it },
                        viewModel = viewModel,
                    )
                }
            }

            composeTestRule.onNodeWithText(context.getString(R.string.profile_runs_title)).assertIsDisplayed()
            val cardMatcher = hasContentDescription("Recorrido Parque", substring = true)
            composeTestRule.onNode(hasScrollAction()).performScrollToNode(cardMatcher)
            composeTestRule.onNode(cardMatcher).assertIsDisplayed().performClick()
            assertEquals("run-101", clickedRunId)
        }

    @Test
    fun `when settings button is clicked, then notifies onSettingsClick`() {
        val viewModel = ProfileViewModel(FakeSavedRunRepository())
        var settingsClicked = false

        composeTestRule.setContent {
            TrazoTheme {
                ProfileScreen(
                    onSettingsClick = { settingsClicked = true },
                    viewModel = viewModel,
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.profile_settings_description))
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertEquals(true, settingsClicked)
    }
}
