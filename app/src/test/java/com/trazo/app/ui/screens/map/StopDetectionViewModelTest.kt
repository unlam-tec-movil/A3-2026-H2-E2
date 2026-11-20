package com.trazo.app.ui.screens.map

import com.trazo.app.domain.model.stops.BrakeEvent
import com.trazo.app.domain.model.stops.QueryStatus
import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopKind
import com.trazo.app.domain.model.stops.StopPhase
import com.trazo.app.domain.repository.StopMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StopDetectionViewModelTest {
    private class FakeStopMonitor : StopMonitor {
        val flow = MutableStateFlow(StopDetectionState())
        val classifications = mutableListOf<StopKind>()
        override val state: StateFlow<StopDetectionState> = flow

        override fun answer(kind: StopKind) {
            classifications += kind
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeStopMonitor()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun brake(id: String) = BrakeEvent(id = id, runId = "r1", location = null, detectedAtMillis = 0L)

    private fun braking(
        id: String,
        query: QueryStatus = QueryStatus.NOT_DUE,
    ) = StopDetectionState(
        phase = StopPhase.Braking(sinceMillis = 0L, brake = brake(id), query = query),
    )

    private fun test(block: suspend TestScope.(StopDetectionViewModel) -> Unit) =
        runTest(dispatcher) {
            val viewModel = StopDetectionViewModel(repository)
            runCurrent()
            block(viewModel)
        }

    @Test
    fun initialState_hasNoAlertNorDialog() =
        test { viewModel ->
            assertEquals(StopDetectionUiState(), viewModel.uiState.value)
        }

    @Test
    fun newBrake_showsAlert_andClearsAtFourSeconds() =
        test { viewModel ->
            repository.flow.value = braking("b1")
            runCurrent()
            assertEquals(BrakeAlertUi("b1"), viewModel.uiState.value.alert)

            advanceTimeBy(3_999)
            assertNotNull(viewModel.uiState.value.alert)

            advanceTimeBy(1)
            runCurrent()
            assertNull(viewModel.uiState.value.alert)
        }

    @Test
    fun alertCleared_doesNotReappearForSameBrake() =
        test { viewModel ->
            repository.flow.value = braking("b1")
            advanceTimeBy(4_000)
            runCurrent()
            assertNull(viewModel.uiState.value.alert)

            repository.flow.value = braking("b1", QueryStatus.ANSWERED)
            runCurrent()
            assertNull(viewModel.uiState.value.alert)
        }

    @Test
    fun alertIsHidden_whenQueryBecomesPending() =
        test { viewModel ->
            repository.flow.value = braking("b1")
            runCurrent()
            repository.flow.value = braking("b1", QueryStatus.PENDING)
            runCurrent()

            assertNull(viewModel.uiState.value.alert)
        }

    @Test
    fun alertIsHidden_whenAnsweredAsPersonalPause() =
        test { viewModel ->
            repository.flow.value = braking("b1")
            runCurrent()
            repository.flow.value = StopDetectionState(phase = StopPhase.PersonalPause(sinceMillis = 0L))
            runCurrent()

            assertNull(viewModel.uiState.value.alert)
        }

    @Test
    fun secondBrake_replacesAlert() =
        test { viewModel ->
            repository.flow.value = braking("b1")
            runCurrent()
            repository.flow.value = StopDetectionState()
            runCurrent()
            advanceTimeBy(1_000)
            repository.flow.value = braking("b2")
            runCurrent()

            assertEquals(BrakeAlertUi("b2"), viewModel.uiState.value.alert)
            advanceTimeBy(3_000)
            assertNotNull(viewModel.uiState.value.alert)
        }

    @Test
    fun existingBrakesAtStart_doNotShowAlert() =
        runTest(dispatcher) {
            repository.flow.value = braking("old")
            val viewModel = StopDetectionViewModel(repository)
            runCurrent()

            assertNull(viewModel.uiState.value.alert)
        }

    @Test
    fun dialog_followsPendingClassification() =
        test { viewModel ->
            repository.flow.value = braking("b1", QueryStatus.PENDING)
            runCurrent()
            assertTrue(viewModel.uiState.value.classificationVisible)

            repository.flow.value = braking("b1", QueryStatus.ANSWERED)
            runCurrent()
            assertFalse(viewModel.uiState.value.classificationVisible)
        }

    @Test
    fun dialog_closesWhenRunnerResumesWithoutAnswering() =
        test { viewModel ->
            repository.flow.value = braking("b1", QueryStatus.PENDING)
            runCurrent()
            repository.flow.value = StopDetectionState()
            runCurrent()

            assertFalse(viewModel.uiState.value.classificationVisible)
        }

    @Test
    fun onClassify_delegatesToStopMonitor() =
        test { viewModel ->
            viewModel.onClassify(StopKind.PERSONAL_PAUSE)

            assertEquals(listOf(StopKind.PERSONAL_PAUSE), repository.classifications)
        }

    @Test
    fun suppressed_hidesAlertAndDialog() =
        test { viewModel ->
            repository.flow.value = braking("b1")
            runCurrent()
            viewModel.onSuppressedChanged(true)
            runCurrent()
            assertNull(viewModel.uiState.value.alert)

            repository.flow.value = braking("b1", QueryStatus.PENDING)
            runCurrent()
            assertFalse(viewModel.uiState.value.classificationVisible)
        }

    @Test
    fun suppressed_resolvesPendingQueryAsBrake() =
        test { viewModel ->
            repository.flow.value = braking("b1", QueryStatus.PENDING)
            runCurrent()
            assertTrue(repository.classifications.isEmpty())

            viewModel.onSuppressedChanged(true)
            runCurrent()

            assertEquals(listOf(StopKind.BRAKE), repository.classifications)
        }

    @Test
    fun suppressed_queryBecomingPendingWhileActive_resolvesAsBrake() =
        test { viewModel ->
            viewModel.onSuppressedChanged(true)
            runCurrent()
            repository.flow.value = braking("b1", QueryStatus.PENDING)
            runCurrent()

            assertEquals(listOf(StopKind.BRAKE), repository.classifications)
        }

    @Test
    fun suppressed_brakeDuringSuppression_noLateAlertAfterwards() =
        test { viewModel ->
            viewModel.onSuppressedChanged(true)
            runCurrent()
            repository.flow.value = braking("b1")
            runCurrent()
            assertNull(viewModel.uiState.value.alert)

            viewModel.onSuppressedChanged(false)
            runCurrent()

            assertNull(viewModel.uiState.value.alert)
        }

    @Test
    fun notSuppressed_neverClassifiesOnItsOwn() =
        test { _ ->
            repository.flow.value = braking("b1", QueryStatus.PENDING)
            runCurrent()

            assertTrue(repository.classifications.isEmpty())
        }
}
