package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.stops.MotionSample
import com.trazo.app.domain.model.stops.StopDetectionConfig
import com.trazo.app.domain.model.stops.StopPhase
import com.trazo.app.domain.repository.FakeRunSessionRepository
import com.trazo.app.domain.repository.MotionSource
import com.trazo.app.domain.repository.StopMonitor
import com.trazo.app.domain.usecases.RecordStopsUseCase
import com.trazo.app.domain.usecases.SaveStopUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica, sin modificar `stop-detection`, que la pausa manual (el id en curso pasa a `null`) y la
 * reanudación no generan frenadas ni pausas personales (escenarios E8 y E9).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StopDetectionManualPauseTest {
    private class FakeMotionSource : MotionSource {
        private val shared = MutableSharedFlow<MotionSample>()

        override fun samples(): Flow<MotionSample> = shared

        suspend fun emit(sample: MotionSample) = shared.emit(sample)
    }

    private val motion = FakeMotionSource()
    private val run = FakeRunSessionRepository()
    private val brakes = InMemoryBrakeRepository()
    private var nextId = 0

    private val saved get() = brakes.brakes.value

    private fun TestScope.repository() =
        SensorStopMonitor(
            motionSource = motion,
            runSessionRepository = run,
            clock = { testScheduler.currentTime },
            scope = backgroundScope,
            config = StopDetectionConfig(),
            idGenerator = { "b${nextId++}" },
        ).also { recordStops(it) }

    /** Guarda las paradas cerradas como en la app: `RecordStopsUseCase` suscripto desde el inicio. */
    private fun TestScope.recordStops(monitor: StopMonitor) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            RecordStopsUseCase(monitor, SaveStopUseCase(brakes))()
        }
    }

    /** Muestras quietas (magnitud constante) cada 100 ms virtuales, siempre; el repositorio decide si las lee. */
    private fun TestScope.emitStillSamplesForever() {
        backgroundScope.launch {
            while (true) {
                motion.emit(MotionSample(testScheduler.currentTime, 9.8f))
                delay(100)
            }
        }
    }

    private fun startRun() {
        run.setActive("r1", routePoints = listOf(RoutePoint(-34.6, -58.4, 5f, 0f, 0L)))
    }

    private fun TestScope.advance(millis: Long) {
        advanceTimeBy(millis)
        runCurrent()
    }

    @Test
    fun `pausar durante una detencion de menos de 8 s no registra frenada ni pausa personal`() =
        runTest {
            val repository = repository()
            startRun()
            emitStillSamplesForever()
            advance(5_000)

            run.pauseSession()
            advance(60_000)

            val state = repository.state.value
            assertTrue(saved.isEmpty())
            assertEquals(StopPhase.Moving, state.phase)
            assertTrue(run.appliedStates.isEmpty())
        }

    @Test
    fun `pausar durante una frenada la guarda y reanudar no la duplica`() =
        runTest {
            val repository = repository()
            startRun()
            emitStillSamplesForever()
            advance(10_000)
            assertTrue(repository.state.value.phase is StopPhase.Braking)
            assertTrue(saved.isEmpty())

            run.pauseSession()
            advance(1_000)
            assertEquals(1, saved.size)

            run.resumeSession()
            advance(100)
            assertEquals(1, saved.size)
        }

    @Test
    fun `al reanudar la fase es movimiento y la quietud anterior no se arrastra`() =
        runTest {
            val repository = repository()
            startRun()
            emitStillSamplesForever()
            advance(5_000)
            run.pauseSession()
            advance(1_000)

            run.resumeSession()
            advance(100)
            assertEquals(StopPhase.Moving, repository.state.value.phase)

            // 5 s quieto antes de la pausa + 5 s después sumarían 10 s si se arrastrara (> 8 s).
            advance(5_000)

            assertTrue(repository.state.value.phase !is StopPhase.Braking)
            assertTrue(saved.isEmpty())
        }
}
