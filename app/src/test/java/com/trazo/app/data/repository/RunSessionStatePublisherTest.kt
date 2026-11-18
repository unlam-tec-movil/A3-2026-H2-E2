package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Clock

class RunSessionStatePublisherTest {
    /** Replica los orígenes permitidos acordados con Persona 3 (D1-D3); falla fuera de ellos. */
    private class FakeRunSessionRepository : com.trazo.app.domain.repository.RunSessionRepository {
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

        override suspend fun finishSession(): Result<FrozenRunSummary> = error("no se usa")

        override fun resetToIdle() = error("no se usa")

        override fun getFinishedSummary(id: String): FrozenRunSummary? = error("no se usa")
    }

    private val repository = FakeRunSessionRepository()
    private val publisher = RunSessionStatePublisher(repository)

    private fun active(
        state: RunState,
        id: String = "run-1",
    ) = RunSession.ActiveSession(
        id = id,
        goal = RunGoal.FIVE_KILOMETERS,
        state = state,
        startMonotonicTimestamp = 0L,
        startedAt = Clock.System.now(),
    )

    private fun current() = (repository.session.value as RunSession.ActiveSession).state

    @Test
    fun `STOPPED desde ACTIVE llama markStopped`() =
        runTest {
            repository.session.value = active(RunState.ACTIVE)
            assertTrue(publisher.publish("run-1", RunState.STOPPED).isSuccess)
            assertEquals(listOf("markStopped"), repository.calls)
            assertEquals(RunState.STOPPED, current())
        }

    @Test
    fun `ACTIVE desde STOPPED llama markMoving`() =
        runTest {
            repository.session.value = active(RunState.STOPPED)
            assertTrue(publisher.publish("run-1", RunState.ACTIVE).isSuccess)
            assertEquals(listOf("markMoving"), repository.calls)
            assertEquals(RunState.ACTIVE, current())
        }

    @Test
    fun `ACTIVE desde PERSONAL_PAUSED llama markMoving`() =
        runTest {
            repository.session.value = active(RunState.PERSONAL_PAUSED)
            assertTrue(publisher.publish("run-1", RunState.ACTIVE).isSuccess)
            assertEquals(listOf("markMoving"), repository.calls)
            assertEquals(RunState.ACTIVE, current())
        }

    @Test
    fun `PERSONAL_PAUSED desde STOPPED llama pausePersonally`() =
        runTest {
            repository.session.value = active(RunState.STOPPED)
            assertTrue(publisher.publish("run-1", RunState.PERSONAL_PAUSED).isSuccess)
            assertEquals(listOf("pausePersonally"), repository.calls)
            assertEquals(RunState.PERSONAL_PAUSED, current())
        }

    @Test
    fun `no pisa la pausa manual ni llama al repositorio`() =
        runTest {
            repository.session.value = active(RunState.PAUSED)
            assertTrue(publisher.publish("run-1", RunState.ACTIVE).isFailure)
            assertTrue(publisher.publish("run-1", RunState.STOPPED).isFailure)
            assertTrue(publisher.publish("run-1", RunState.PERSONAL_PAUSED).isFailure)
            assertTrue(repository.calls.isEmpty())
            assertEquals(RunState.PAUSED, current())
        }

    @Test
    fun `id distinto o sesion inactiva fallan sin llamar`() =
        runTest {
            repository.session.value = active(RunState.ACTIVE, id = "otro")
            assertTrue(publisher.publish("run-1", RunState.STOPPED).isFailure)
            repository.session.value = RunSession.Idle
            assertTrue(publisher.publish("run-1", RunState.STOPPED).isFailure)
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun `combinaciones no traducibles fallan sin llamar`() =
        runTest {
            repository.session.value = active(RunState.ACTIVE)
            assertTrue(publisher.publish("run-1", RunState.ACTIVE).isFailure)
            assertTrue(publisher.publish("run-1", RunState.PERSONAL_PAUSED).isFailure)
            assertTrue(publisher.publish("run-1", RunState.PAUSED).isFailure)
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun `Result failure del repositorio se devuelve y el siguiente ciclo reintenta`() =
        runTest {
            repository.session.value = active(RunState.STOPPED)
            // El estado leído era STOPPED pero cambia antes de la llamada: el repositorio rechaza.
            val racing =
                object : com.trazo.app.domain.repository.RunSessionRepository by repository {
                    override suspend fun markMoving(): Result<Unit> {
                        repository.session.value = active(RunState.PAUSED)
                        return repository.markMoving()
                    }
                }
            val racingPublisher = RunSessionStatePublisher(racing)

            assertTrue(racingPublisher.publish("run-1", RunState.ACTIVE).isFailure)
            assertEquals(RunState.PAUSED, current())

            // Siguiente ciclo: la sesión vuelve a STOPPED y el reintento sí se aplica.
            repository.session.value = active(RunState.STOPPED)
            assertTrue(publisher.publish("run-1", RunState.ACTIVE).isSuccess)
            assertEquals(RunState.ACTIVE, current())
        }

    @Test
    fun `nunca usa resumeSession`() =
        runTest {
            repository.session.value = active(RunState.PERSONAL_PAUSED)
            publisher.publish("run-1", RunState.ACTIVE)
            assertTrue("resumeSession" !in repository.calls)
        }
}
