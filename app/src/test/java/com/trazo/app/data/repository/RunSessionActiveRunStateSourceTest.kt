package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.domain.model.regularity.ActiveRunSnapshot
import com.trazo.app.domain.repository.RunSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Clock

class RunSessionActiveRunStateSourceTest {
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
    private val source = RunSessionActiveRunStateSource(repository)

    private fun active(state: RunState) =
        RunSession.ActiveSession(
            id = "run-1",
            goal = RunGoal.FIVE_KILOMETERS,
            state = state,
            startMonotonicTimestamp = 0L,
            startedAt = Clock.System.now(),
        )

    @Test
    fun `sin sesion no hay recorrido`() =
        runTest {
            assertNull(source.activeRun.first())
        }

    @Test
    fun `cada estado de la sesion activa se refleja con su id`() =
        runTest {
            listOf(
                RunState.ACTIVE,
                RunState.PAUSED,
                RunState.STOPPED,
                RunState.PERSONAL_PAUSED,
            ).forEach { state ->
                repository.session.value = active(state)
                assertEquals(ActiveRunSnapshot("run-1", state), source.activeRun.first())
            }
        }

    @Test
    fun `al volver a inactiva deja de haber recorrido`() =
        runTest {
            repository.session.value = active(RunState.ACTIVE)
            repository.session.value = RunSession.Idle
            assertNull(source.activeRun.first())
        }
}
