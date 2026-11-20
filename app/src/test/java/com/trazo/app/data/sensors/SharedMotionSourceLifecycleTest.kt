package com.trazo.app.data.sensors

import com.trazo.app.data.repository.SensorRegularityMonitor
import com.trazo.app.data.repository.SensorStopMonitor
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.model.stops.MotionSample
import com.trazo.app.domain.model.stops.StopDetectionConfig
import com.trazo.app.domain.repository.FakeRunSessionRepository
import com.trazo.app.domain.repository.MotionSensorAvailability
import com.trazo.app.domain.repository.MotionSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Ciclo de vida del sensor con ambos consumidores (`stop-detection` y regularidad) sobre el
 * [SharedMotionSource] real y un origen falso que cuenta los listeners activos (escenarios E8 a E12).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedMotionSourceLifecycleTest {
    private class CountingUpstream : MotionSource {
        private val shared = MutableSharedFlow<MotionSample>()
        var activeListeners = 0
            private set

        override fun samples(): Flow<MotionSample> =
            shared
                .onStart { activeListeners++ }
                .onCompletion { activeListeners-- }

        suspend fun emit(sample: MotionSample) = shared.emit(sample)
    }

    private val upstream = CountingUpstream()

    /** Un único recorrido que ven ambos consumidores; PAUSED = pausa manual. */
    private val run = FakeRunSessionRepository()

    private fun TestScope.startBothConsumers() {
        val shared = SharedMotionSource(upstream, backgroundScope)
        val clock = { testScheduler.currentTime }
        SensorStopMonitor(
            motionSource = shared,
            runSessionRepository = run,
            clock = clock,
            scope = backgroundScope,
            config = StopDetectionConfig(),
        )
        SensorRegularityMonitor(
            motionSource = shared,
            runSessionRepository = run,
            availability = MotionSensorAvailability { true },
            clock = clock,
            scope = backgroundScope,
        )
        backgroundScope.launch {
            while (true) {
                upstream.emit(MotionSample(testScheduler.currentTime, 9.8f))
                delay(100)
            }
        }
    }

    private fun TestScope.advance(millis: Long) {
        advanceTimeBy(millis)
        runCurrent()
    }

    private fun setRun(
        id: String?,
        state: RunState = RunState.ACTIVE,
    ) {
        if (id == null) run.resetToIdle() else run.setActive(id, state)
    }

    @Test
    fun `sin recorrido no hay listener`() =
        runTest {
            startBothConsumers()

            advance(5_000)

            assertEquals(0, upstream.activeListeners)
        }

    @Test
    fun `con el recorrido activo hay un unico listener aunque lean los dos consumidores`() =
        runTest {
            startBothConsumers()

            setRun("r1")
            advance(5_000)

            assertEquals(1, upstream.activeListeners)
        }

    @Test
    fun `la pausa manual da de baja el listener y reanudar registra uno`() =
        runTest {
            startBothConsumers()
            setRun("r1")
            advance(5_000)

            setRun("r1", RunState.PAUSED)
            advance(5_000)
            assertEquals(0, upstream.activeListeners)

            setRun("r1", RunState.ACTIVE)
            advance(5_000)
            assertEquals(1, upstream.activeListeners)
        }

    @Test
    fun `al finalizar el recorrido no queda listener`() =
        runTest {
            startBothConsumers()
            setRun("r1")
            advance(5_000)

            setRun(null)
            advance(5_000)

            assertEquals(0, upstream.activeListeners)
        }

    @Test
    fun `al descartar un recorrido pausado no queda listener y uno nuevo registra uno`() =
        runTest {
            startBothConsumers()
            setRun("r1")
            advance(5_000)
            setRun("r1", RunState.PAUSED)
            advance(5_000)

            setRun(null)
            advance(5_000)
            assertEquals(0, upstream.activeListeners)

            setRun("r2")
            advance(5_000)
            assertEquals(1, upstream.activeListeners)
        }
}
