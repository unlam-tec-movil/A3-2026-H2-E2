package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunMetrics
import com.trazo.app.domain.repository.FakeRunSessionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import kotlin.time.Clock

class GetFinishedRunSummaryUseCaseTest {
    private lateinit var repository: FakeRunSessionRepository
    private lateinit var useCase: GetFinishedRunSummaryUseCase

    @Before
    fun setUp() {
        repository = FakeRunSessionRepository()
        useCase = GetFinishedRunSummaryUseCase(repository)
    }

    @Test
    fun `returns the summary stored for the id`() {
        val summary = summary("run-1")
        repository.putFinishedSummary(summary)

        assertSame(summary, useCase("run-1"))
    }

    @Test
    fun `returns null for an unknown id`() {
        repository.putFinishedSummary(summary("run-1"))

        assertNull(useCase("unknown"))
    }

    @Test
    fun `only reads and does not clear the summary`() {
        val summary = summary("run-1")
        repository.putFinishedSummary(summary)

        useCase("run-1")

        // Una segunda lectura sigue devolviendo el resumen: el caso de uso no limpia nada.
        assertSame(summary, useCase("run-1"))
        assertEquals(listOf("run-1", "run-1"), repository.getFinishedSummaryCalls)
    }

    private fun summary(id: String) =
        FrozenRunSummary(
            id = id,
            goal = RunGoal.FIVE_KILOMETERS,
            routePoints = emptyList(),
            metrics = RunMetrics(elapsedTimeSeconds = 1930L, distanceMeters = 5200.0),
            startedAt = Clock.System.now(),
            startedAtMonotonicTimestamp = 1000L,
            finishedAtMonotonicTimestamp = 2000L,
            goalReached = true,
        )
}
