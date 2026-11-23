package com.trazo.app.domain.usecases

import com.trazo.app.data.repository.BrakeRepository
import com.trazo.app.domain.model.stops.BrakeEvent
import com.trazo.app.domain.model.stops.ClosedStop
import com.trazo.app.domain.model.stops.StopKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveStopUseCaseTest {
    private class FakeBrakeRepository : BrakeRepository {
        val saved = mutableListOf<BrakeEvent>()
        override val brakes: StateFlow<List<BrakeEvent>> = MutableStateFlow(emptyList())

        override fun save(brake: BrakeEvent) {
            saved += brake
        }
    }

    private val repository = FakeBrakeRepository()
    private val saveStop = SaveStopUseCase(repository)
    private val brake = BrakeEvent(id = "b1", runId = "r1", location = null, detectedAtMillis = 8_000L)

    @Test
    fun `contestada como frenada se guarda`() {
        saveStop(ClosedStop(brake, StopKind.BRAKE))

        assertEquals(listOf(brake), repository.saved)
    }

    @Test
    fun `sin respuesta cuenta como frenada y se guarda`() {
        saveStop(ClosedStop(brake, answer = null))

        assertEquals(listOf(brake), repository.saved)
    }

    @Test
    fun `una pausa personal no llama al repositorio`() {
        saveStop(ClosedStop(brake, StopKind.PERSONAL_PAUSE))

        assertTrue(repository.saved.isEmpty())
    }
}
