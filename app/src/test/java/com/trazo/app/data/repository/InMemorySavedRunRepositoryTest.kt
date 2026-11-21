package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.saved.SavedRun
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

class InMemorySavedRunRepositoryTest {
    private fun run(
        id: String,
        startedAtSeconds: Long,
    ) = SavedRun(
        id = id,
        title = "Recorrido $id",
        startedAt = Instant.fromEpochSeconds(startedAtSeconds),
        goal = RunGoal.THREE_KILOMETERS,
        goalReached = true,
        isDemo = false,
        distanceMeters = 3000.0,
        elapsedTimeSeconds = 1200,
        averageSpeedKmh = 9.0,
        routePoints = emptyList(),
        brakes = emptyList(),
        regularityPercent = null,
        highlights = emptyList(),
        photoPaths = emptyList(),
    )

    @Test
    fun `sin recorridos guardados la lista esta vacia`() =
        runTest {
            val repository = InMemorySavedRunRepository()

            assertEquals(emptyList<SavedRun>(), repository.runs.first())
        }

    @Test
    fun `los recorridos se listan del mas reciente al mas antiguo`() =
        runTest {
            val repository = InMemorySavedRunRepository()
            repository.save(run("viejo", startedAtSeconds = 100))
            repository.save(run("nuevo", startedAtSeconds = 200))

            val ids = repository.runs.first().map { it.id }

            assertEquals(listOf("nuevo", "viejo"), ids)
        }
}
