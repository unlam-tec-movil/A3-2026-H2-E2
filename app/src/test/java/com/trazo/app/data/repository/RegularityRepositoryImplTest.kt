package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.model.regularity.ActiveRunSnapshot
import com.trazo.app.domain.model.regularity.RegularityConfig
import com.trazo.app.domain.model.stops.MotionSample
import com.trazo.app.domain.repository.ActiveRunStateSource
import com.trazo.app.domain.repository.MotionSensorAvailability
import com.trazo.app.domain.repository.MotionSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegularityRepositoryImplTest {
    private class FakeMotionSource : MotionSource {
        private val shared = MutableSharedFlow<MotionSample>()
        var activeCollectors = 0
            private set

        override fun samples(): Flow<MotionSample> =
            shared
                .onStart { activeCollectors++ }
                .onCompletion { activeCollectors-- }

        suspend fun emit(sample: MotionSample) = shared.emit(sample)
    }

    private class FakeActiveRunStateSource : ActiveRunStateSource {
        val run = MutableStateFlow<ActiveRunSnapshot?>(null)
        override val activeRun: Flow<ActiveRunSnapshot?> = run
    }

    private val motion = FakeMotionSource()
    private val runs = FakeActiveRunStateSource()
    private val config = RegularityConfig()

    private fun TestScope.repository(
        available: Boolean = true,
        source: MotionSource = motion,
    ) = RegularityRepositoryImpl(
        motionSource = source,
        activeRunStateSource = runs,
        availability = MotionSensorAvailability { available },
        clock = { testScheduler.currentTime },
        scope = backgroundScope,
        config = config,
    )

    /** Emite muestras parejas (magnitud constante: vibración baja) cada 100 ms hasta [untilMillis]. */
    private fun TestScope.emitSamplesUntil(untilMillis: Long) {
        backgroundScope.launch {
            while (testScheduler.currentTime < untilMillis) {
                motion.emit(MotionSample(testScheduler.currentTime, 9.8f))
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
        runs.run.value = id?.let { ActiveRunSnapshot(it, state) }
    }

    @Test
    fun `sin recorrido no se lee el sensor ni hay tramos`() =
        runTest {
            val repository = repository()
            emitSamplesUntil(30_000)

            advance(30_000)

            assertEquals(0, motion.activeCollectors)
            assertEquals(0, repository.state.value.completedSegments)
            assertNull(repository.state.value.percentage)
        }

    @Test
    fun `en activo cuenta un tramo cada 10 segundos`() =
        runTest {
            val repository = repository()
            setRun("r1")
            emitSamplesUntil(40_000)

            advance(35_000)

            val state = repository.state.value
            assertEquals("r1", state.runId)
            assertEquals(3, state.completedSegments)
            assertEquals(3, state.lowVibrationSegments)
            assertEquals(100, state.percentage)
        }

    @Test
    fun `pausa manual descarta el tramo abierto, conserva los cerrados y deja de leer`() =
        runTest {
            val repository = repository()
            setRun("r1")
            emitSamplesUntil(100_000)
            advance(25_000)
            assertEquals(2, repository.state.value.completedSegments)

            setRun("r1", RunState.PAUSED)
            advance(30_000)

            assertEquals(0, motion.activeCollectors)
            assertEquals(2, repository.state.value.completedSegments)
        }

    @Test
    fun `al reanudar el tramo empieza de cero y conserva los previos`() =
        runTest {
            val repository = repository()
            setRun("r1")
            emitSamplesUntil(100_000)
            advance(25_000)
            setRun("r1", RunState.PAUSED)
            advance(1_000)

            setRun("r1", RunState.ACTIVE)
            // 5 s tras reanudar: el tramo abierto antes de la pausa (5 s) no se arrastra.
            advance(5_000)
            assertEquals(2, repository.state.value.completedSegments)

            advance(6_000)
            assertEquals(3, repository.state.value.completedSegments)
        }

    @Test
    fun `detenido por frenada y en pausa personal no suman ni leen`() =
        runTest {
            val repository = repository()
            setRun("r1")
            emitSamplesUntil(100_000)
            advance(15_000)
            assertEquals(1, repository.state.value.completedSegments)

            setRun("r1", RunState.STOPPED)
            advance(30_000)
            assertEquals(1, repository.state.value.completedSegments)
            assertEquals(0, motion.activeCollectors)

            setRun("r1", RunState.PERSONAL_PAUSED)
            advance(30_000)
            assertEquals(1, repository.state.value.completedSegments)
            assertEquals(0, motion.activeCollectors)
        }

    @Test
    fun `un id nuevo reinicia el estado`() =
        runTest {
            val repository = repository()
            setRun("r1")
            emitSamplesUntil(100_000)
            advance(25_000)
            assertEquals(2, repository.state.value.completedSegments)

            setRun(null)
            advance(100)
            setRun("r2")
            advance(100)

            assertEquals("r2", repository.state.value.runId)
            assertEquals(0, repository.state.value.completedSegments)
            assertEquals(0, repository.state.value.lowVibrationSegments)
        }

    @Test
    fun `el valor sigue disponible al pasar a sin recorrido y se deja de leer`() =
        runTest {
            val repository = repository()
            setRun("r1")
            emitSamplesUntil(100_000)
            advance(25_000)

            setRun(null)
            advance(30_000)

            assertEquals(0, motion.activeCollectors)
            assertEquals("r1", repository.state.value.runId)
            assertEquals(2, repository.state.value.completedSegments)
            assertEquals(100, repository.state.value.percentage)
        }

    @Test
    fun `mismo id tras sin recorrido conserva los tramos`() =
        runTest {
            val repository = repository()
            setRun("r1")
            emitSamplesUntil(100_000)
            advance(25_000)
            setRun(null)
            advance(100)

            setRun("r1")
            advance(100)

            assertEquals(2, repository.state.value.completedSegments)
        }

    @Test
    fun `sin sensor no hay tramos, el porcentaje es null y no falla`() =
        runTest {
            val emptySource =
                object : MotionSource {
                    override fun samples(): Flow<MotionSample> = emptyFlow()
                }
            val repository = repository(available = false, source = emptySource)
            setRun("r1")

            advance(60_000)

            assertFalse(repository.state.value.sensorAvailable)
            assertEquals(0, repository.state.value.completedSegments)
            assertNull(repository.state.value.percentage)
        }

    @Test
    fun `el reinicio por id nuevo conserva la disponibilidad del sensor`() =
        runTest {
            val repository = repository(available = false)
            setRun("r1")
            advance(100)
            setRun(null)
            advance(100)

            setRun("r2")
            advance(100)

            assertFalse(repository.state.value.sensorAvailable)
        }
}
