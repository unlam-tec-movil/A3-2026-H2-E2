package com.trazo.app.domain.usecases

import com.trazo.app.data.repository.FakeRunSessionRepository
import com.trazo.app.data.repository.InMemoryBrakeRepository
import com.trazo.app.data.sensors.MotionSensorAvailability
import com.trazo.app.data.sensors.MotionSource
import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.stops.BrakeEvent
import com.trazo.app.domain.model.stops.ClosedStop
import com.trazo.app.domain.model.stops.MotionSample
import com.trazo.app.domain.model.stops.QueryStatus
import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopKind
import com.trazo.app.domain.model.stops.StopPhase
import com.trazo.app.domain.services.RegularityMonitor
import com.trazo.app.domain.services.StopMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FinishRunUseCaseTest {
    private class FakeStopMonitor : StopMonitor {
        val flow = MutableStateFlow(StopDetectionState())
        override val state: StateFlow<StopDetectionState> = flow
        override val closedStops: Flow<ClosedStop> = emptyFlow()

        override fun answer(kind: StopKind) = Unit
    }

    private class FakeMotionSource : MotionSource {
        val shared = MutableSharedFlow<MotionSample>()

        override fun samples(): Flow<MotionSample> = shared
    }

    private val runs = FakeRunSessionRepository()
    private val brakes = InMemoryBrakeRepository()
    private val stops = FakeStopMonitor()
    private val motion = FakeMotionSource()

    private fun TestScope.useCase(sensorAvailable: Boolean = true): FinishRunUseCase {
        val regularity =
            RegularityMonitor(
                motionSource = motion,
                runSessionRepository = runs,
                availability = MotionSensorAvailability { sensorAvailable },
                clock = { testScheduler.currentTime },
                scope = backgroundScope,
            )
        return FinishRunUseCase(runs, brakes, stops, regularity)
    }

    private fun brake(
        id: String,
        runId: String = RUN_ID,
        atMillis: Long = 0L,
    ) = BrakeEvent(id = id, runId = runId, location = null, detectedAtMillis = atMillis)

    private fun ids(summary: FrozenRunSummary) = summary.brakes.orEmpty().map { it.id }

    private fun braking(brake: BrakeEvent) =
        StopDetectionState(StopPhase.Braking(sinceMillis = 0L, brake = brake, query = QueryStatus.PENDING))

    /** Emite muestras parejas (vibración baja) cada 100 ms durante [millis] y avanza el reloj. */
    private fun TestScope.runSteadily(millis: Long) {
        val until = testScheduler.currentTime + millis
        backgroundScope.launch {
            while (testScheduler.currentTime < until) {
                motion.shared.emit(MotionSample(testScheduler.currentTime, 9.8f))
                delay(100)
            }
        }
        advanceTimeBy(millis)
        runCurrent()
    }

    @Test
    fun `cuenta solo las frenadas guardadas del recorrido`() =
        runTest {
            val finish = useCase()
            runs.setActive(RUN_ID)
            brakes.save(brake("b2", atMillis = 2_000L))
            brakes.save(brake("b1", atMillis = 1_000L))
            brakes.save(brake("otra", runId = "run-viejo"))

            val summary = finish().getOrThrow()

            assertEquals(listOf("b1", "b2"), ids(summary))
            assertEquals(2, summary.brakeCount)
        }

    @Test
    fun `una frenada abierta al finalizar cuenta aunque todavia no se haya guardado`() =
        runTest {
            val finish = useCase()
            runs.setActive(RUN_ID)
            brakes.save(brake("b1", atMillis = 1_000L))
            stops.flow.value = braking(brake("abierta", atMillis = 5_000L))

            val summary = finish().getOrThrow()

            assertEquals(listOf("b1", "abierta"), ids(summary))
        }

    @Test
    fun `una frenada abierta que ya se guardo no se cuenta dos veces`() =
        runTest {
            val finish = useCase()
            runs.setActive(RUN_ID)
            brakes.save(brake("b1"))
            stops.flow.value = braking(brake("b1"))

            val summary = finish().getOrThrow()

            assertEquals(listOf("b1"), ids(summary))
        }

    @Test
    fun `una frenada abierta de otro recorrido no cuenta`() =
        runTest {
            val finish = useCase()
            runs.setActive(RUN_ID)
            stops.flow.value = braking(brake("ajena", runId = "run-viejo"))

            val summary = finish().getOrThrow()

            assertEquals(emptyList<String>(), ids(summary))
        }

    @Test
    fun `congela la regularidad medida del recorrido`() =
        runTest {
            val finish = useCase()
            runs.setActive(RUN_ID)
            runCurrent()
            runSteadily(25_000)

            val summary = finish().getOrThrow()

            assertEquals(100, summary.regularityPercent)
            assertEquals(summary, runs.getFinishedSummary(RUN_ID))
        }

    @Test
    fun `sin tramos medidos o sin sensor la regularidad queda nula`() =
        runTest {
            val finish = useCase(sensorAvailable = false)
            runs.setActive(RUN_ID)
            runCurrent()
            runSteadily(25_000)

            assertNull(finish().getOrThrow().regularityPercent)
        }

    @Test
    fun `sin acelerometro las frenadas quedan sin dato y no en cero`() =
        runTest {
            val finish = useCase(sensorAvailable = false)
            runs.setActive(RUN_ID)
            brakes.save(brake("b1"))

            val summary = finish().getOrThrow()

            assertNull(summary.brakes)
            assertNull(summary.brakeCount)
        }

    @Test
    fun `si falla el cierre devuelve el fallo y la sesion sigue activa`() =
        runTest {
            val finish = useCase()
            runs.setActive(RUN_ID)
            runs.failNextFinish = true

            val result = finish()

            assertTrue(result.isFailure)
            assertTrue(runs.isRunInProgress.value)
        }

    @Test
    fun `sin recorrido activo falla`() =
        runTest {
            assertTrue(useCase()().isFailure)
        }

    private companion object {
        const val RUN_ID = "run-1"
    }
}
