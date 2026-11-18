package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.model.stops.MotionSample
import com.trazo.app.domain.model.stops.StopDetectionConfig
import com.trazo.app.domain.model.stops.StopLocation
import com.trazo.app.domain.model.stops.StopPhase
import com.trazo.app.domain.repository.MotionSource
import com.trazo.app.domain.repository.RunStatePublisher
import com.trazo.app.domain.repository.RunningRunSource
import com.trazo.app.domain.repository.StopLocationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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

    private class FakeRunningRunSource : RunningRunSource {
        val id = MutableStateFlow<String?>(null)
        override val runningRunId: Flow<String?> = id
    }

    private class FakePublisher : RunStatePublisher {
        val published = mutableListOf<RunState>()

        override suspend fun publish(
            runId: String,
            state: RunState,
        ): Result<Unit> {
            published += state
            return Result.success(Unit)
        }
    }

    private val motion = FakeMotionSource()
    private val run = FakeRunningRunSource()
    private val publisher = FakePublisher()
    private var nextId = 0

    private fun TestScope.repository() =
        StopDetectionRepositoryImpl(
            motionSource = motion,
            runningRunSource = run,
            locationProvider =
                object : StopLocationProvider {
                    override fun lastKnown() = StopLocation(-34.6, -58.4, 5f)
                },
            publisher = publisher,
            clock = { testScheduler.currentTime },
            scope = backgroundScope,
            config = StopDetectionConfig(),
            idGenerator = { "b${nextId++}" },
        )

    /** Muestras quietas (magnitud constante) cada 100 ms virtuales, siempre; el repositorio decide si las lee. */
    private fun TestScope.emitStillSamplesForever() {
        backgroundScope.launch {
            while (true) {
                motion.emit(MotionSample(testScheduler.currentTime, 9.8f))
                delay(100)
            }
        }
    }

    private fun TestScope.advance(millis: Long) {
        advanceTimeBy(millis)
        runCurrent()
    }

    @Test
    fun `pausar durante una detencion de menos de 8 s no registra frenada ni pausa personal`() =
        runTest {
            val repository = repository()
            run.id.value = "r1"
            emitStillSamplesForever()
            advance(5_000)

            run.id.value = null
            advance(60_000)

            val state = repository.state.value
            assertTrue(state.brakes.isEmpty())
            assertEquals(StopPhase.Moving, state.phase)
            assertTrue(publisher.published.isEmpty())
        }

    @Test
    fun `las frenadas previas se conservan al pausar y al reanudar`() =
        runTest {
            val repository = repository()
            run.id.value = "r1"
            emitStillSamplesForever()
            advance(10_000)
            assertEquals(1, repository.state.value.brakes.size)

            run.id.value = null
            advance(1_000)
            assertEquals(1, repository.state.value.brakes.size)

            run.id.value = "r1"
            advance(100)
            assertEquals(1, repository.state.value.brakes.size)
        }

    @Test
    fun `al reanudar la fase es movimiento y la quietud anterior no se arrastra`() =
        runTest {
            val repository = repository()
            run.id.value = "r1"
            emitStillSamplesForever()
            advance(5_000)
            run.id.value = null
            advance(1_000)

            run.id.value = "r1"
            advance(100)
            assertEquals(StopPhase.Moving, repository.state.value.phase)

            // 5 s quieto antes de la pausa + 5 s después sumarían 10 s si se arrastrara (> 8 s).
            advance(5_000)

            assertTrue(
                repository.state.value.brakes
                    .isEmpty(),
            )
        }
}
