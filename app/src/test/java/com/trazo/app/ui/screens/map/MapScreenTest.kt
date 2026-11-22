package com.trazo.app.ui.screens.map

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.R
import com.trazo.app.data.repository.FakeLocationRepository
import com.trazo.app.data.repository.FakeRunSessionRepository
import com.trazo.app.data.repository.FakeSavedRunRepository
import com.trazo.app.domain.model.LocationStatus
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.model.saved.SavedRun
import com.trazo.app.domain.usecases.testFinishRunUseCase
import com.trazo.app.ui.theme.TrazoTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Clock
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class MapScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository = FakeRunSessionRepository()
    private var startRunClicks = 0

    private val savedRunRepository = FakeSavedRunRepository()
    private var locationStatus = LocationStatus.READY
    private val silentLocationRepository = FakeLocationRepository(locationStatus = { locationStatus })

    private fun createViewModel() =
        LiveRouteTrackingViewModel(
            runSessionRepository = repository,
            locationRepository = silentLocationRepository,
            finishRun = testFinishRunUseCase(repository),
            savedRunRepository = savedRunRepository,
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

    private fun savedRun(
        id: String,
        startedAtSeconds: Long = 1_700_000_000L,
    ) = SavedRun(
        id = id,
        title = "Recorrido $id",
        startedAt = Instant.fromEpochSeconds(startedAtSeconds),
        goal = RunGoal.FIVE_KILOMETERS,
        goalReached = true,
        isDemo = false,
        distanceMeters = 5_200.0,
        elapsedTimeSeconds = 3_300L,
        averageSpeedKmh = 9.5,
        routePoints = listOf(tracePoint(0), tracePoint(1)),
        brakes = emptyList(),
        regularityPercent = 80,
        highlights = emptyList(),
        photoPaths = emptyList(),
    )

    private fun tracePoint(index: Int) =
        RoutePoint(
            latitude = -34.6 + index * 0.001,
            longitude = -58.4,
            accuracyMeters = 5f,
            speedMetersPerSecond = 2f,
            monotonicTimestampMillis = index.toLong(),
        )

    @Test
    fun `given saved traces in the state, when converting them for the map layer, then there is one entry per run`() {
        // Given
        val traces =
            listOf(
                SavedTraceUi(id = "b", points = listOf(tracePoint(0), tracePoint(1))),
                SavedTraceUi(id = "a", points = listOf(tracePoint(2), tracePoint(3))),
            )

        // When
        val forLayer = traces.toMapLayerTraces()

        // Then
        assertEquals(mapOf("b" to traces[0].points, "a" to traces[1].points), forLayer)
    }

    @Test
    fun `given no saved traces in the state, when converting them for the map layer, then nothing is passed`() {
        assertTrue(emptyList<SavedTraceUi>().toMapLayerTraces().isEmpty())
    }

    @Test
    fun `given saved runs and a precise location, when the screen is shown, then the state carries their traces`() {
        // Given
        runBlocking { savedRunRepository.save(savedRun("a")) }
        val viewModel = createViewModel()

        // When
        showScreen(viewModel)
        composeTestRule.waitForIdle()

        // Then
        assertEquals(listOf("a"), viewModel.traceIds())
    }

    @Test
    fun `given no saved runs, when the screen is shown, then the state carries no traces`() {
        // Given
        val viewModel = createViewModel()

        // When
        showScreen(viewModel)
        composeTestRule.waitForIdle()

        // Then
        assertTrue(viewModel.traceIds().isEmpty())
    }

    @Test
    fun `given the screen comes back to the foreground, when the location became precise, then it rereads it`() {
        // Given
        runBlocking { savedRunRepository.save(savedRun("a")) }
        locationStatus = LocationStatus.APPROXIMATE_ONLY
        val viewModel = createViewModel()
        val owner = TestLifecycleOwner()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                TrazoTheme { MapScreen(onStartRunClick = {}, onNavigateToResults = {}, viewModel = viewModel) }
            }
        }
        composeTestRule.runOnIdle { owner.moveTo(Lifecycle.State.RESUMED) }
        composeTestRule.waitForIdle()
        assertTrue(viewModel.traceIds().isEmpty())

        // When
        locationStatus = LocationStatus.READY
        composeTestRule.runOnIdle {
            owner.moveTo(Lifecycle.State.STARTED)
            owner.moveTo(Lifecycle.State.RESUMED)
        }
        composeTestRule.waitForIdle()

        // Then
        assertEquals(listOf("a"), viewModel.traceIds())
    }

    private fun LiveRouteTrackingViewModel.traceIds() = uiState.value.savedTraces.map { it.id }

    private class TestLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry

        fun moveTo(state: Lifecycle.State) {
            registry.currentState = state
        }
    }
}
