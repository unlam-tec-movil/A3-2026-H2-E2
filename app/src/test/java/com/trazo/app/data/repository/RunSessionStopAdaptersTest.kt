package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.model.stops.StopLocation
import com.trazo.app.domain.repository.RunSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Clock

class RunSessionStopAdaptersTest {
    private class FakeRunSessionRepository : RunSessionRepository {
        val session = MutableStateFlow<RunSession>(RunSession.Idle)
        override val activeSession: StateFlow<RunSession> = session
        override val isRunInProgress: StateFlow<Boolean> = MutableStateFlow(false)

        override fun attachActiveSession(session: InitializedRunSession) = error("no se usa")

        override suspend fun pauseSession(): Result<Unit> = error("no se usa")

        override suspend fun pausePersonally(): Result<Unit> = error("no se usa")

        override suspend fun markStopped(): Result<Unit> = error("no se usa")

        override suspend fun markMoving(): Result<Unit> = error("no se usa")

        override suspend fun resumeSession(): Result<Unit> = error("no se usa")

        override suspend fun finishSession(): Result<FrozenRunSummary> = error("no se usa")

        override fun resetToIdle() = error("no se usa")

        override fun getFinishedSummary(id: String): FrozenRunSummary? = error("no se usa")
    }

    private val repository = FakeRunSessionRepository()
    private val runningRun = RunSessionRunningRunSource(repository)
    private val location = RunSessionStopLocationProvider(repository)

    private fun active(
        state: RunState,
        points: List<RoutePoint> = emptyList(),
    ) = RunSession.ActiveSession(
        id = "run-1",
        goal = RunGoal.FIVE_KILOMETERS,
        state = state,
        routePoints = points,
        startMonotonicTimestamp = 0L,
        startedAt = Clock.System.now(),
    )

    private fun point(
        lat: Double,
        lon: Double,
        accuracy: Float = 5f,
    ) = RoutePoint(lat, lon, accuracy, 2f, 0L)

    @Test
    fun `sesion inactiva no entrega id`() =
        runTest {
            assertNull(runningRun.runningRunId.first())
        }

    @Test
    fun `pausa manual no entrega id`() =
        runTest {
            repository.session.value = active(RunState.PAUSED)
            assertNull(runningRun.runningRunId.first())
        }

    @Test
    fun `activa, detenida y pausa personal entregan el id`() =
        runTest {
            listOf(RunState.ACTIVE, RunState.STOPPED, RunState.PERSONAL_PAUSED).forEach { state ->
                repository.session.value = active(state)
                assertEquals("run-1", runningRun.runningRunId.first())
            }
        }

    @Test
    fun `al finalizar el id vuelve a null`() =
        runTest {
            repository.session.value = active(RunState.ACTIVE)
            repository.session.value = RunSession.Idle
            assertNull(runningRun.runningRunId.first())
        }

    @Test
    fun `sin sesion la ubicacion es null`() {
        assertNull(location.lastKnown())
    }

    @Test
    fun `sin puntos la ubicacion es null`() {
        repository.session.value = active(RunState.ACTIVE)
        assertNull(location.lastKnown())
    }

    @Test
    fun `la ubicacion es el ultimo punto de la traza`() {
        repository.session.value =
            active(RunState.STOPPED, listOf(point(1.0, 2.0), point(-34.6, -58.4, 7f)))
        assertEquals(StopLocation(-34.6, -58.4, 7f), location.lastKnown())
    }
}
