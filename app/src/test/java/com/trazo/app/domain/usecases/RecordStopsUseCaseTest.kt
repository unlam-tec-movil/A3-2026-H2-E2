package com.trazo.app.domain.usecases

import com.trazo.app.data.repository.BrakeRepository
import com.trazo.app.domain.model.stops.BrakeEvent
import com.trazo.app.domain.model.stops.ClosedStop
import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopKind
import com.trazo.app.domain.services.StopMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordStopsUseCaseTest {
    private class FakeStopMonitor : StopMonitor {
        val closed = MutableSharedFlow<ClosedStop>(extraBufferCapacity = 8)
        override val state: StateFlow<StopDetectionState> = MutableStateFlow(StopDetectionState())
        override val closedStops: Flow<ClosedStop> = closed

        override fun answer(kind: StopKind) = Unit
    }

    private class FakeBrakeRepository : BrakeRepository {
        val saved = mutableListOf<BrakeEvent>()
        override val brakes: StateFlow<List<BrakeEvent>> = MutableStateFlow(emptyList())

        override fun save(brake: BrakeEvent) {
            saved += brake
        }
    }

    private val monitor = FakeStopMonitor()
    private val repository = FakeBrakeRepository()
    private val recordStops = RecordStopsUseCase(monitor, SaveStopUseCase(repository))

    private fun brake(id: String) = BrakeEvent(id = id, runId = "r1", location = null, detectedAtMillis = 8_000L)

    @Test
    fun `cada parada cerrada pasa por la clasificacion y solo se guardan las frenadas`() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { recordStops() }

            monitor.closed.emit(ClosedStop(brake("b1"), StopKind.BRAKE))
            monitor.closed.emit(ClosedStop(brake("b2"), StopKind.PERSONAL_PAUSE))
            monitor.closed.emit(ClosedStop(brake("b3"), answer = null))

            assertEquals(listOf("b1", "b3"), repository.saved.map { it.id })
        }
}
