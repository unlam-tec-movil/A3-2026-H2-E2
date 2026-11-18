package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.model.stops.MotionSample
import com.trazo.app.domain.model.stops.QueryStatus
import com.trazo.app.domain.model.stops.StopDetectionConfig
import com.trazo.app.domain.model.stops.StopKind
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StopDetectionRepositoryImplTest {
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

    private class FakeRunningRunSource : RunningRunSource {
        val id = MutableStateFlow<String?>(null)
        override val runningRunId: Flow<String?> = id
    }

    private class FakePublisher : RunStatePublisher {
        val published = mutableListOf<Pair<String, RunState>>()
        var failNext = 0

        override suspend fun publish(
            runId: String,
            state: RunState,
        ): Result<Unit> {
            if (failNext > 0) {
                failNext--
                return Result.failure(IllegalStateException("origen no válido"))
            }
            published += runId to state
            return Result.success(Unit)
        }
    }

    private val location = StopLocation(-34.6, -58.4, 5f)
    private val locationProvider =
        object : StopLocationProvider {
            override fun lastKnown() = location
        }
    private val config = StopDetectionConfig()
    private val motion = FakeMotionSource()
    private val run = FakeRunningRunSource()
    private val publisher = FakePublisher()
    private var nextId = 0

    private fun TestScope.repository() =
        StopDetectionRepositoryImpl(
            motionSource = motion,
            runningRunSource = run,
            locationProvider = locationProvider,
            publisher = publisher,
            clock = { testScheduler.currentTime },
            scope = backgroundScope,
            config = config,
            idGenerator = { "b${nextId++}" },
        )

    /** Emite muestras quietas (magnitud constante) cada 100 ms virtuales hasta [untilMillis]. */
    private fun TestScope.emitStillSamplesUntil(untilMillis: Long) {
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

    @Test
    fun `sin recorrido en curso no se lee el sensor y el estado no cambia`() =
        runTest {
            val repository = repository()
            emitStillSamplesUntil(30_000)

            advance(30_000)

            assertEquals(0, motion.activeCollectors)
            assertEquals(StopPhase.Moving, repository.state.value.phase)
            assertTrue(
                repository.state.value.brakes
                    .isEmpty(),
            )
        }

    @Test
    fun `ocho segundos quieto registra una frenada con ubicacion`() =
        runTest {
            val repository = repository()
            run.id.value = "r1"
            emitStillSamplesUntil(20_000)

            advance(10_000)

            val state = repository.state.value
            assertEquals(1, state.brakes.size)
            assertEquals(location, state.brakes.single().location)
            assertTrue(state.phase is StopPhase.Braking)
        }

    @Test
    fun `mantenerse quieto no registra una segunda frenada`() =
        runTest {
            val repository = repository()
            run.id.value = "r1"
            emitStillSamplesUntil(40_000)

            advance(19_000)

            assertEquals(1, repository.state.value.brakes.size)
        }

    @Test
    fun `al dejar de haber recorrido se deja de leer el sensor y se conservan las frenadas`() =
        runTest {
            val repository = repository()
            run.id.value = "r1"
            emitStillSamplesUntil(60_000)
            advance(10_000)
            assertEquals(1, motion.activeCollectors)

            run.id.value = null
            advance(100)

            assertEquals(0, motion.activeCollectors)
            assertEquals(StopPhase.Moving, repository.state.value.phase)
            assertEquals(1, repository.state.value.brakes.size)
        }

    @Test
    fun `el mismo id conserva las frenadas y otro id las vacia`() =
        runTest {
            val repository = repository()
            run.id.value = "r1"
            emitStillSamplesUntil(120_000)
            advance(10_000)
            run.id.value = null
            advance(100)

            run.id.value = "r1"
            advance(100)
            assertEquals(1, repository.state.value.brakes.size)

            run.id.value = null
            advance(100)
            run.id.value = "r2"
            advance(100)
            assertTrue(
                repository.state.value.brakes
                    .isEmpty(),
            )
            assertEquals(StopPhase.Moving, repository.state.value.phase)
        }

    @Test
    fun `un sensor mudo tras quieto no registra frenada`() =
        runTest {
            val repository = repository()
            run.id.value = "r1"
            emitStillSamplesUntil(2_000)

            advance(30_000)

            assertTrue(
                repository.state.value.brakes
                    .isEmpty(),
            )
            assertEquals(StopPhase.Moving, repository.state.value.phase)
        }

    @Test
    fun `clasificar como pausa personal quita la frenada y no se vuelve a registrar`() =
        runTest {
            val repository = repository()
            run.id.value = "r1"
            emitStillSamplesUntil(60_000)
            advance(22_000)
            val pending = (repository.state.value.phase as StopPhase.Braking)
            assertEquals(QueryStatus.PENDING, pending.query)
            assertNotNull(repository.state.value.pendingClassificationBrakeId)

            repository.classifyCurrentStop(StopKind.PERSONAL_PAUSE)
            advance(20_000)

            assertTrue(repository.state.value.phase is StopPhase.PersonalPause)
            assertTrue(
                repository.state.value.brakes
                    .isEmpty(),
            )
        }

    @Test
    fun `clasificar como frenada concurrente con los ticks no se pierde`() =
        runTest {
            val repository = repository()
            run.id.value = "r1"
            emitStillSamplesUntil(60_000)
            advance(22_000)

            backgroundScope.launch { repository.classifyCurrentStop(StopKind.BRAKE) }
            advance(5_000)

            val phase = repository.state.value.phase as StopPhase.Braking
            assertEquals(QueryStatus.ANSWERED, phase.query)
            assertEquals(1, repository.state.value.brakes.size)
        }

    @Test
    fun `una frenada publica STOPPED una sola vez`() =
        runTest {
            repository()
            run.id.value = "r1"
            emitStillSamplesUntil(60_000)

            advance(15_000)

            assertEquals(listOf("r1" to RunState.STOPPED), publisher.published)
        }

    @Test
    fun `clasificar como pausa personal publica PERSONAL_PAUSED`() =
        runTest {
            val repository = repository()
            run.id.value = "r1"
            emitStillSamplesUntil(60_000)
            advance(22_000)

            repository.classifyCurrentStop(StopKind.PERSONAL_PAUSE)
            advance(1_000)

            assertEquals(
                listOf(RunState.STOPPED, RunState.PERSONAL_PAUSED),
                publisher.published.map { it.second },
            )
        }

    @Test
    fun `retomar el movimiento publica ACTIVE`() =
        runTest {
            repository()
            run.id.value = "r1"
            emitStillSamplesUntil(10_000)
            advance(10_000)
            assertEquals(listOf(RunState.STOPPED), publisher.published.map { it.second })

            // El sensor calla: sin evidencia se trata como movimiento y tras 1,5 s se retoma.
            advance(5_000)

            assertEquals(
                listOf(RunState.STOPPED, RunState.ACTIVE),
                publisher.published.map { it.second },
            )
        }

    @Test
    fun `con pausa manual no se publica nada`() =
        runTest {
            repository()
            run.id.value = "r1"
            emitStillSamplesUntil(60_000)
            advance(10_000)
            run.id.value = null
            advance(100)
            val before = publisher.published.size

            advance(20_000)

            assertEquals(before, publisher.published.size)
            assertFalse(publisher.published.any { it.second == RunState.PAUSED })
        }

    @Test
    fun `si el puerto rechaza la publicacion se reintenta en el siguiente ciclo`() =
        runTest {
            repository()
            publisher.failNext = 2
            run.id.value = "r1"
            emitStillSamplesUntil(60_000)

            advance(12_000)

            assertEquals(listOf("r1" to RunState.STOPPED), publisher.published)
        }
}
