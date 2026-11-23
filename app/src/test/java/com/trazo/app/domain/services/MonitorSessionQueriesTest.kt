package com.trazo.app.domain.services

import com.trazo.app.data.repository.RunSessionRepository
import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.model.stops.BrakeEvent
import com.trazo.app.domain.model.stops.StopLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

/**
 * Consultas y traducciones sobre [RunSessionRepository] que usan los monitores: `runningRunId`,
 * `lastKnownStopLocation` y `publishStopState` ([SensorStopMonitor]) y `activeRunSnapshot`
 * ([RegularityMonitor]).
 */
class MonitorSessionQueriesTest {
    /** Replica los orígenes permitidos acordados con Persona 3 (D1-D3); falla fuera de ellos. */
    private class DetectionOriginsRepository : RunSessionRepository {
        val session = MutableStateFlow<RunSession>(RunSession.Idle)
        val calls = mutableListOf<String>()
        override val activeSession: StateFlow<RunSession> = session
        override val isRunInProgress: StateFlow<Boolean> = MutableStateFlow(false)

        private fun transition(
            name: String,
            allowedFrom: Set<RunState>,
            to: RunState,
        ): Result<Unit> {
            calls += name
            val current = session.value as? RunSession.ActiveSession
            if (current == null || current.state !in allowedFrom) {
                return Result.failure(IllegalStateException("Origen no permitido para $name"))
            }
            session.value = current.copy(state = to)
            return Result.success(Unit)
        }

        override fun attachActiveSession(session: InitializedRunSession) = error("no se usa")

        override suspend fun pauseSession(): Result<Unit> = error("no se usa")

        override suspend fun pausePersonally() =
            transition("pausePersonally", setOf(RunState.ACTIVE, RunState.STOPPED), RunState.PERSONAL_PAUSED)

        override suspend fun markStopped() = transition("markStopped", setOf(RunState.ACTIVE), RunState.STOPPED)

        override suspend fun markMoving() = transition("markMoving", setOf(RunState.STOPPED, RunState.PERSONAL_PAUSED), RunState.ACTIVE)

        override suspend fun resumeSession(): Result<Unit> {
            calls += "resumeSession"
            return Result.success(Unit)
        }

        override suspend fun finishSession(
            brakes: List<BrakeEvent>?,
            regularityPercent: Int?,
        ): Result<FrozenRunSummary> = error("no se usa")

        override fun removeFinishedSummary(id: String): Boolean = error("no se usa")

        override fun resetToIdle() = error("no se usa")

        override fun getFinishedSummary(id: String): FrozenRunSummary? = error("no se usa")
    }

    private val repository = DetectionOriginsRepository()

    private fun active(
        state: RunState,
        id: String = "run-1",
        points: List<RoutePoint> = emptyList(),
    ) = RunSession.ActiveSession(
        id = id,
        goal = RunGoal.FIVE_KILOMETERS,
        state = state,
        routePoints = points,
        startMonotonicTimestamp = 0L,
        startedAt = Instant.fromEpochMilliseconds(0L),
    )

    private fun point(
        lat: Double,
        lon: Double,
        accuracy: Float = 5f,
    ) = RoutePoint(lat, lon, accuracy, 2f, 0L)

    private fun current() = (repository.session.value as RunSession.ActiveSession).state

    // runningRunId

    @Test
    fun `sesion inactiva no entrega id`() =
        runTest {
            assertNull(repository.runningRunId.first())
        }

    @Test
    fun `pausa manual no entrega id`() =
        runTest {
            repository.session.value = active(RunState.PAUSED)
            assertNull(repository.runningRunId.first())
        }

    @Test
    fun `activa, detenida y pausa personal entregan el id`() =
        runTest {
            listOf(RunState.ACTIVE, RunState.STOPPED, RunState.PERSONAL_PAUSED).forEach { state ->
                repository.session.value = active(state)
                assertEquals("run-1", repository.runningRunId.first())
            }
        }

    @Test
    fun `al finalizar el id vuelve a null`() =
        runTest {
            repository.session.value = active(RunState.ACTIVE)
            repository.session.value = RunSession.Idle
            assertNull(repository.runningRunId.first())
        }

    // lastKnownStopLocation

    @Test
    fun `sin sesion la ubicacion es null`() {
        assertNull(repository.lastKnownStopLocation())
    }

    @Test
    fun `sin puntos la ubicacion es null`() {
        repository.session.value = active(RunState.ACTIVE)
        assertNull(repository.lastKnownStopLocation())
    }

    @Test
    fun `la ubicacion es el ultimo punto de la traza`() {
        repository.session.value = active(RunState.STOPPED, points = listOf(point(1.0, 2.0), point(-34.6, -58.4, 7f)))
        assertEquals(StopLocation(-34.6, -58.4, 7f), repository.lastKnownStopLocation())
    }

    // activeRunSnapshot

    @Test
    fun `sin sesion no hay recorrido`() =
        runTest {
            assertNull(repository.activeRunSnapshot.first())
        }

    @Test
    fun `cada estado de la sesion activa se refleja con su id`() =
        runTest {
            listOf(RunState.ACTIVE, RunState.PAUSED, RunState.STOPPED, RunState.PERSONAL_PAUSED).forEach { state ->
                repository.session.value = active(state)
                assertEquals(ActiveRunSnapshot("run-1", state), repository.activeRunSnapshot.first())
            }
        }

    @Test
    fun `al volver a inactiva deja de haber recorrido`() =
        runTest {
            repository.session.value = active(RunState.ACTIVE)
            repository.session.value = RunSession.Idle
            assertNull(repository.activeRunSnapshot.first())
        }

    // publishStopState

    @Test
    fun `STOPPED desde ACTIVE llama markStopped`() =
        runTest {
            repository.session.value = active(RunState.ACTIVE)
            assertTrue(repository.publishStopState("run-1", RunState.STOPPED).isSuccess)
            assertEquals(listOf("markStopped"), repository.calls)
            assertEquals(RunState.STOPPED, current())
        }

    @Test
    fun `ACTIVE desde STOPPED llama markMoving`() =
        runTest {
            repository.session.value = active(RunState.STOPPED)
            assertTrue(repository.publishStopState("run-1", RunState.ACTIVE).isSuccess)
            assertEquals(listOf("markMoving"), repository.calls)
            assertEquals(RunState.ACTIVE, current())
        }

    @Test
    fun `ACTIVE desde PERSONAL_PAUSED llama markMoving`() =
        runTest {
            repository.session.value = active(RunState.PERSONAL_PAUSED)
            assertTrue(repository.publishStopState("run-1", RunState.ACTIVE).isSuccess)
            assertEquals(listOf("markMoving"), repository.calls)
            assertEquals(RunState.ACTIVE, current())
        }

    @Test
    fun `PERSONAL_PAUSED desde STOPPED llama pausePersonally`() =
        runTest {
            repository.session.value = active(RunState.STOPPED)
            assertTrue(repository.publishStopState("run-1", RunState.PERSONAL_PAUSED).isSuccess)
            assertEquals(listOf("pausePersonally"), repository.calls)
            assertEquals(RunState.PERSONAL_PAUSED, current())
        }

    @Test
    fun `no pisa la pausa manual ni llama al repositorio`() =
        runTest {
            repository.session.value = active(RunState.PAUSED)
            assertTrue(repository.publishStopState("run-1", RunState.ACTIVE).isFailure)
            assertTrue(repository.publishStopState("run-1", RunState.STOPPED).isFailure)
            assertTrue(repository.publishStopState("run-1", RunState.PERSONAL_PAUSED).isFailure)
            assertTrue(repository.calls.isEmpty())
            assertEquals(RunState.PAUSED, current())
        }

    @Test
    fun `id distinto o sesion inactiva fallan sin llamar`() =
        runTest {
            repository.session.value = active(RunState.ACTIVE, id = "otro")
            assertEquals("El recorrido cambió.", repository.publishStopState("run-1", RunState.STOPPED).exceptionOrNull()?.message)
            repository.session.value = RunSession.Idle
            assertEquals("No hay un recorrido activo.", repository.publishStopState("run-1", RunState.STOPPED).exceptionOrNull()?.message)
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun `ACTIVE desde ACTIVE es exito sin llamar, asi no se reintenta tras reanudar a mano`() =
        runTest {
            repository.session.value = active(RunState.ACTIVE)
            assertTrue(repository.publishStopState("run-1", RunState.ACTIVE).isSuccess)
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun `combinaciones no traducibles fallan sin llamar`() =
        runTest {
            repository.session.value = active(RunState.ACTIVE)
            assertTrue(repository.publishStopState("run-1", RunState.PERSONAL_PAUSED).isFailure)
            assertEquals(
                "Cambio de ACTIVE a PAUSED no soportado.",
                repository.publishStopState("run-1", RunState.PAUSED).exceptionOrNull()?.message,
            )
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun `Result failure del repositorio se devuelve y el siguiente ciclo reintenta`() =
        runTest {
            repository.session.value = active(RunState.STOPPED)
            // El estado leído era STOPPED pero cambia antes de la llamada: el repositorio rechaza.
            val racing =
                object : RunSessionRepository by repository {
                    override suspend fun markMoving(): Result<Unit> {
                        repository.session.value = active(RunState.PAUSED)
                        return repository.markMoving()
                    }
                }

            assertTrue(racing.publishStopState("run-1", RunState.ACTIVE).isFailure)
            assertEquals(RunState.PAUSED, current())

            // Siguiente ciclo: la sesión vuelve a STOPPED y el reintento sí se aplica.
            repository.session.value = active(RunState.STOPPED)
            assertTrue(repository.publishStopState("run-1", RunState.ACTIVE).isSuccess)
            assertEquals(RunState.ACTIVE, current())
        }

    @Test
    fun `nunca usa resumeSession`() =
        runTest {
            repository.session.value = active(RunState.PERSONAL_PAUSED)
            repository.publishStopState("run-1", RunState.ACTIVE)
            assertTrue("resumeSession" !in repository.calls)
        }
}
