package com.trazo.app.domain.usecases

import com.trazo.app.data.repository.FakeRunSessionRepository
import com.trazo.app.data.repository.FakeSavedRunRepository
import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunMetrics
import com.trazo.app.domain.model.saved.SavedRun
import com.trazo.app.domain.model.stops.BrakeEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import kotlin.time.Instant

class SaveRunUseCaseTest {
    private val runSessions = FakeRunSessionRepository()
    private val savedRuns = FakeSavedRunRepository()
    private val useCase = SaveRunUseCase(runSessions, savedRuns)

    @Before
    fun setUp() {
        runSessions.putFinishedSummary(summary())
    }

    @Test
    fun `guarda el recorrido con lo medido y lo que cargo el usuario`() =
        runTest {
            val result =
                useCase("r1", "  Mi carrera  ", listOf("Sin parar"), listOf("/fotos/1.jpg"))

            assertEquals(SaveRunResult.Saved, result)
            val saved = savedRuns.getById("r1")
            assertEquals("Mi carrera", saved?.title)
            assertEquals(5000.0, saved?.distanceMeters)
            assertEquals(80, saved?.regularityPercent)
            assertEquals(1, saved?.brakeCount)
            assertEquals(listOf("Sin parar"), saved?.highlights)
        }

    @Test
    fun `un titulo de menos de 3 caracteres es invalido y no guarda`() =
        runTest {
            val result = useCase("r1", "  ab  ", emptyList(), emptyList())

            assertEquals(SaveRunResult.InvalidTitle, result)
            assertNull(savedRuns.getById("r1"))
        }

    @Test
    fun `un titulo de mas de 50 caracteres es invalido`() =
        runTest {
            val result = useCase("r1", "a".repeat(51), emptyList(), emptyList())

            assertEquals(SaveRunResult.InvalidTitle, result)
        }

    @Test
    fun `mas fotos que el maximo no se guarda`() =
        runTest {
            val photos = List(SavedRun.MAX_PHOTOS + 1) { "/fotos/$it.jpg" }

            val result = useCase("r1", "Mi carrera", emptyList(), photos)

            assertEquals(SaveRunResult.TooManyPhotos, result)
            assertNull(savedRuns.getById("r1"))
        }

    @Test
    fun `un recorrido que no existe devuelve RunNotFound`() =
        runTest {
            val result = useCase("otro", "Mi carrera", emptyList(), emptyList())

            assertEquals(SaveRunResult.RunNotFound, result)
        }

    @Test
    fun `sin acelerometro las frenadas sin dato se guardan como lista vacia`() =
        runTest {
            runSessions.putFinishedSummary(summary().copy(brakes = null))

            val result = useCase("r1", "Mi carrera", emptyList(), emptyList())

            assertEquals(SaveRunResult.Saved, result)
            assertEquals(emptyList<BrakeEvent>(), savedRuns.getById("r1")?.brakes)
        }

    private fun summary() =
        FrozenRunSummary(
            id = "r1",
            goal = RunGoal.FIVE_KILOMETERS,
            routePoints = emptyList(),
            metrics = RunMetrics(elapsedTimeSeconds = 1800, distanceMeters = 5000.0, averageSpeedKmh = 10.0),
            startedAt = Instant.fromEpochMilliseconds(0L),
            startedAtMonotonicTimestamp = 0L,
            finishedAtMonotonicTimestamp = 2000L,
            goalReached = true,
            isDemoMode = true,
            brakes = listOf(BrakeEvent("b1", "r1", null, 500L)),
            regularityPercent = 80,
        )
}
