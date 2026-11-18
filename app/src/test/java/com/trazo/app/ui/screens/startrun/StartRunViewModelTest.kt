package com.trazo.app.ui.screens.startrun

import com.trazo.app.domain.model.LocationStatus
import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RawLocationPoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.repository.FakeRunSessionRepository
import com.trazo.app.domain.repository.LocationClient
import com.trazo.app.domain.usecases.AttachActiveRunSessionUseCase
import com.trazo.app.domain.usecases.AwaitPreciseLocationUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StartRunViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRunSessionRepository()
    private var locationStatus = LocationStatus.READY
    private var locationUpdates: Flow<RawLocationPoint> = MutableSharedFlow()
    private val startedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L)
    private val monotonicNow = 42_000L

    private val locationClient =
        object : LocationClient {
            override fun getLocationUpdates(intervalMillis: Long): Flow<RawLocationPoint> = locationUpdates

            override fun getSignalQualityUpdates(): Flow<GpsQuality> = emptyFlow()
        }

    private lateinit var viewModel: StartRunViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel =
            StartRunViewModel(
                runSessionRepository = repository,
                locationStatusReader = { locationStatus },
                awaitPreciseLocation = AwaitPreciseLocationUseCase(locationClient),
                attachActiveRunSession = AttachActiveRunSessionUseCase(repository),
                monotonicClock = { monotonicNow },
                clock =
                    object : Clock {
                        override fun now(): Instant = startedAt
                    },
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun point(accuracy: Float) =
        RawLocationPoint(
            latitude = -34.6,
            longitude = -58.4,
            accuracyMeters = accuracy,
            speedMetersPerSecond = 0f,
            monotonicTimestampMillis = 0L,
        )

    private fun TestScope.collectEffects(): List<StartRunEffect> {
        val effects = mutableListOf<StartRunEffect>()
        // Unconfined: el colector arranca ya; advanceUntilIdle no corre backgroundScope por sí solo.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects += it }
        }
        return effects
    }

    private fun openDialog() {
        viewModel.onFabClick()
        viewModel.onOptionClick()
    }

    private fun inProgressSession() =
        InitializedRunSession(
            id = "run",
            goal = RunGoal.FIVE_KILOMETERS,
            startMonotonicTimestamp = 0L,
            startedAt = startedAt,
        )

    // --- «+» y opción (9.x) ---

    @Test
    fun initialState_fabEnabled_optionAndDialogHidden() =
        runTest(dispatcher) {
            advanceUntilIdle()
            val state = viewModel.uiState.value

            assertTrue(state.isFabEnabled)
            assertFalse(state.isOptionVisible)
            assertFalse(state.isDialogVisible)
            assertNull(state.selectedGoal)
            assertFalse(state.isWaitingForGps)
        }

    // 9.2
    @Test
    fun onFabClick_togglesTheOption() =
        runTest(dispatcher) {
            viewModel.onFabClick()
            assertTrue(viewModel.uiState.value.isOptionVisible)

            viewModel.onFabClick()
            assertFalse(viewModel.uiState.value.isOptionVisible)
        }

    // 9.3
    @Test
    fun onOptionDismiss_hidesTheOptionWithoutOpeningTheDialog() =
        runTest(dispatcher) {
            viewModel.onFabClick()
            viewModel.onOptionDismiss()

            assertFalse(viewModel.uiState.value.isOptionVisible)
            assertFalse(viewModel.uiState.value.isDialogVisible)
        }

    // 9.5
    @Test
    fun runInProgress_disablesFab_andHidesTheOption() =
        runTest(dispatcher) {
            viewModel.onFabClick()
            repository.attachActiveSession(inProgressSession())
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isFabEnabled)
            assertFalse(viewModel.uiState.value.isOptionVisible)

            viewModel.onFabClick()
            assertFalse(viewModel.uiState.value.isOptionVisible)
        }

    // 9.5, 9.6
    @Test
    fun pausedRunKeepsFabDisabled_andFinishingEnablesIt() =
        runTest(dispatcher) {
            repository.attachActiveSession(inProgressSession())
            repository.pauseSession()
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isFabEnabled)

            repository.finishSession()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isFabEnabled)
        }

    // --- Ubicación (6, 7, 8.1) ---

    // 9.4, 1.1
    @Test
    fun onOptionClick_withLocationReady_hidesOptionAndOpensDialogWithNoGoal() =
        runTest(dispatcher) {
            openDialog()

            val state = viewModel.uiState.value
            assertFalse(state.isOptionVisible)
            assertTrue(state.isDialogVisible)
            assertNull(state.selectedGoal)
        }

    // 6.1
    @Test
    fun onOptionClick_withPermissionDenied_requestsPermission_andDoesNotOpenDialog() =
        runTest(dispatcher) {
            val effects = collectEffects()
            locationStatus = LocationStatus.PERMISSION_DENIED

            openDialog()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isDialogVisible)
            assertNull(viewModel.uiState.value.locationIssue)
            assertEquals(listOf(StartRunEffect.RequestLocationPermission), effects)
        }

    // 6.1
    @Test
    fun onPermissionResult_granted_opensDialog() =
        runTest(dispatcher) {
            locationStatus = LocationStatus.PERMISSION_DENIED
            openDialog()

            locationStatus = LocationStatus.READY
            viewModel.onPermissionResult()

            assertTrue(viewModel.uiState.value.isDialogVisible)
        }

    // 6.2, 6.3
    @Test
    fun onPermissionResult_denied_showsIssue_andDoesNotOpenDialog() =
        runTest(dispatcher) {
            locationStatus = LocationStatus.PERMISSION_DENIED
            openDialog()
            viewModel.onPermissionResult()

            assertFalse(viewModel.uiState.value.isDialogVisible)
            assertEquals(LocationStatus.PERMISSION_DENIED, viewModel.uiState.value.locationIssue)
        }

    // 7.1, 7.2
    @Test
    fun onOptionClick_withApproximateOnly_showsIssue() =
        runTest(dispatcher) {
            locationStatus = LocationStatus.APPROXIMATE_ONLY
            openDialog()

            assertFalse(viewModel.uiState.value.isDialogVisible)
            assertEquals(LocationStatus.APPROXIMATE_ONLY, viewModel.uiState.value.locationIssue)
        }

    // 8.1
    @Test
    fun onOptionClick_withDeviceLocationOff_showsIssue() =
        runTest(dispatcher) {
            locationStatus = LocationStatus.DEVICE_LOCATION_OFF
            openDialog()

            assertFalse(viewModel.uiState.value.isDialogVisible)
            assertEquals(LocationStatus.DEVICE_LOCATION_OFF, viewModel.uiState.value.locationIssue)
        }

    @Test
    fun onLocationIssueDismiss_clearsTheIssue() =
        runTest(dispatcher) {
            locationStatus = LocationStatus.DEVICE_LOCATION_OFF
            openDialog()
            viewModel.onLocationIssueDismiss()

            assertNull(viewModel.uiState.value.locationIssue)
        }

    // --- Modal (2, 3, 4) ---

    // 2.1
    @Test
    fun onGoalSelected_marksGoalAndEnablesConfirm() =
        runTest(dispatcher) {
            openDialog()
            viewModel.onGoalSelected(RunGoal.FIVE_KILOMETERS)

            assertEquals(RunGoal.FIVE_KILOMETERS, viewModel.uiState.value.selectedGoal)
            assertTrue(viewModel.uiState.value.isConfirmEnabled)
        }

    // 2.2
    @Test
    fun onGoalSelected_withAnotherGoal_keepsOnlyTheNewOne() =
        runTest(dispatcher) {
            openDialog()
            viewModel.onGoalSelected(RunGoal.THREE_KILOMETERS)
            viewModel.onGoalSelected(RunGoal.TEN_KILOMETERS)

            assertEquals(RunGoal.TEN_KILOMETERS, viewModel.uiState.value.selectedGoal)
        }

    // 3.1
    @Test
    fun onConfirm_withoutGoal_keepsDialogOpen_andDoesNotWait() =
        runTest(dispatcher) {
            val effects = collectEffects()
            openDialog()
            viewModel.onConfirm()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isDialogVisible)
            assertFalse(viewModel.uiState.value.isWaitingForGps)
            assertTrue(effects.isEmpty())
        }

    // 4.1, 4.2
    @Test
    fun onDismiss_closesDialog_forgetsGoal_andDoesNotStart() =
        runTest(dispatcher) {
            openDialog()
            viewModel.onGoalSelected(RunGoal.THREE_KILOMETERS)
            viewModel.onDismiss()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isDialogVisible)
            assertNull(viewModel.uiState.value.selectedGoal)
            assertEquals(RunSession.Idle, repository.activeSession.value)
        }

    // --- Espera de señal y arranque (5, 8.2–8.5, 9.7, 10) ---

    // 5.1, 5.2, 8.2, 9.7
    @Test
    fun onConfirm_withGoal_closesDialog_navigatesToMap_andWaitsWithFabDisabled() =
        runTest(dispatcher) {
            val effects = collectEffects()
            openDialog()
            viewModel.onGoalSelected(RunGoal.FIVE_KILOMETERS)
            viewModel.onConfirm()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isDialogVisible)
            assertEquals(RunGoal.FIVE_KILOMETERS, state.waitingGoal)
            assertFalse(state.isFabEnabled)
            assertEquals(listOf(StartRunEffect.NavigateToMap), effects)
        }

    // 8.2
    @Test
    fun impreciseLocation_doesNotStartTheRun() =
        runTest(dispatcher) {
            val updates = MutableSharedFlow<RawLocationPoint>()
            locationUpdates = updates
            openDialog()
            viewModel.onGoalSelected(RunGoal.FIVE_KILOMETERS)
            viewModel.onConfirm()
            advanceUntilIdle()

            updates.emit(point(50f))
            advanceUntilIdle()

            assertEquals(RunSession.Idle, repository.activeSession.value)
            assertTrue(viewModel.uiState.value.isWaitingForGps)
        }

    // 5.3, 8.3, 10.1, 10.2
    @Test
    fun preciseLocation_startsTheRunWithGoalAndClocks_andStopsWaiting() =
        runTest(dispatcher) {
            val updates = MutableSharedFlow<RawLocationPoint>()
            locationUpdates = updates
            openDialog()
            viewModel.onGoalSelected(RunGoal.TEN_KILOMETERS)
            viewModel.onConfirm()
            advanceUntilIdle()

            updates.emit(point(20f))
            advanceUntilIdle()

            val session = repository.activeSession.value as RunSession.ActiveSession
            assertEquals(RunGoal.TEN_KILOMETERS, session.goal)
            assertEquals(startedAt, session.startedAt)
            assertEquals(monotonicNow, session.startMonotonicTimestamp)
            assertTrue(session.id.isNotBlank())
            assertFalse(viewModel.uiState.value.isWaitingForGps)
            assertFalse(viewModel.uiState.value.isFabEnabled)
        }

    // 10.3
    @Test
    fun preciseLocation_startsTheRunOnlyOnce() =
        runTest(dispatcher) {
            val updates = MutableSharedFlow<RawLocationPoint>()
            locationUpdates = updates
            openDialog()
            viewModel.onGoalSelected(RunGoal.FIVE_KILOMETERS)
            viewModel.onConfirm()
            advanceUntilIdle()

            updates.emit(point(10f))
            advanceUntilIdle()
            val firstId = (repository.activeSession.value as RunSession.ActiveSession).id
            updates.emit(point(5f))
            advanceUntilIdle()

            assertEquals(firstId, (repository.activeSession.value as RunSession.ActiveSession).id)
            assertEquals(0, updates.subscriptionCount.value)
        }

    // 8.4
    @Test
    fun onCancelWaiting_stopsWaiting_andDoesNotStart() =
        runTest(dispatcher) {
            val updates = MutableSharedFlow<RawLocationPoint>()
            locationUpdates = updates
            openDialog()
            viewModel.onGoalSelected(RunGoal.FIVE_KILOMETERS)
            viewModel.onConfirm()
            advanceUntilIdle()

            viewModel.onCancelWaiting()
            advanceUntilIdle()
            updates.emit(point(10f))
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isWaitingForGps)
            assertTrue(viewModel.uiState.value.isFabEnabled)
            assertEquals(RunSession.Idle, repository.activeSession.value)
        }

    // 8.5
    @Test
    fun locationErrorWhileWaiting_stopsWaiting_andShowsIssue() =
        runTest(dispatcher) {
            locationUpdates = flow { throw SecurityException("permiso revocado") }
            openDialog()
            viewModel.onGoalSelected(RunGoal.FIVE_KILOMETERS)
            locationStatus = LocationStatus.PERMISSION_DENIED
            viewModel.onConfirm()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isWaitingForGps)
            assertEquals(LocationStatus.PERMISSION_DENIED, viewModel.uiState.value.locationIssue)
            assertEquals(RunSession.Idle, repository.activeSession.value)
        }
}
