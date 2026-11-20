package com.trazo.app.ui.screens.map

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.R
import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.repository.FakeRunSessionRepository
import com.trazo.app.domain.repository.LocationClient
import com.trazo.app.ui.theme.TrazoTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Clock

@RunWith(AndroidJUnit4::class)
class MapScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository = FakeRunSessionRepository()
    private var startRunClicks = 0

    private val silentLocationClient =
        object : LocationClient {
            override fun getLocationUpdates(intervalMillis: Long): Flow<RoutePoint> = emptyFlow()

            override fun getSignalQualityUpdates(): Flow<GpsQuality> = emptyFlow()
        }

    private fun createViewModel() =
        LiveRouteTrackingViewModel(
            runSessionRepository = repository,
            locationClient = silentLocationClient,
        )

    private fun activeSession() =
        RunSession.ActiveSession(
            id = "run-1",
            goal = RunGoal.FIVE_KILOMETERS,
            state = RunState.ACTIVE,
            startMonotonicTimestamp = 0L,
            startedAt = Clock.System.now(),
        )

    private fun showScreen(viewModel: LiveRouteTrackingViewModel = createViewModel()) {
        composeTestRule.setContent {
            TrazoTheme {
                MapScreen(
                    onStartRunClick = { startRunClicks++ },
                    onNavigateToResults = {},
                    startRunFab = { Text("start-run-fab") },
                    gpsWaitingNotice = { Text("gps-waiting") },
                    viewModel = viewModel,
                )
            }
        }
    }

    @Test
    fun `given no active run, when the map screen is shown, then it shows the map pieces and no GPS indicator`() {
        // Given / When
        showScreen()

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.map_attribution)).assertIsDisplayed()
        composeTestRule.onNodeWithText("start-run-fab").assertIsDisplayed()
        composeTestRule.onNodeWithText("gps-waiting").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.map_gps_no_signal)).assertDoesNotExist()
    }

    @Test
    fun `given an active run, when the map screen is shown, then it shows the GPS indicator`() {
        // Given
        repository.setSession(activeSession())

        // When
        showScreen()
        composeTestRule.waitForIdle()

        // Then
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.map_gps_no_signal)).assertIsDisplayed()
    }

    @Test
    fun `given the map screen, when the runner taps recenter, then the screen keeps following the runner`() {
        // Given
        val viewModel = createViewModel()
        showScreen(viewModel)

        // When
        composeTestRule.runOnIdle { viewModel.onMapDragged() }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.map_recenter)).performClick()
        composeTestRule.waitForIdle()

        // Then
        assertTrue(viewModel.uiState.value.isMapCenteredOnRunner)
    }

    @Test
    fun `given the start run effect, when the view model requests the modal, then the screen notifies it`() {
        // Given
        val viewModel = createViewModel()
        showScreen(viewModel)

        // When
        composeTestRule.runOnIdle { viewModel.onStartRunClicked() }
        composeTestRule.waitForIdle()

        // Then
        assertEquals(1, startRunClicks)
    }

    @Test
    fun `given a short run, when requesting to finish, then the short run dialog is shown`() {
        // Given
        repository.setSession(activeSession())
        val viewModel = createViewModel()
        showScreen(viewModel)
        composeTestRule.waitForIdle()

        // When
        composeTestRule.runOnIdle { viewModel.onFinishRequested() }
        composeTestRule.waitForIdle()

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.map_short_run_title)).assertExists()
    }

    @Test
    fun `given the short run dialog, when discarding the run, then the session returns to idle`() {
        // Given
        repository.setSession(activeSession())
        val viewModel = createViewModel()
        showScreen(viewModel)
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { viewModel.onFinishRequested() }
        composeTestRule.waitForIdle()

        // When
        composeTestRule.onNodeWithText(context.getString(R.string.map_discard)).performClick()
        composeTestRule.waitForIdle()

        // Then
        assertEquals(RunSession.Idle, repository.activeSession.value)
    }
}
