package com.trazo.app.domain.model.saved

import com.trazo.app.domain.model.map.RunGoal
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

class SavedRunBadgesTest {
    private fun run(
        goalReached: Boolean,
        isDemo: Boolean,
    ) = SavedRun(
        id = "a",
        title = "Recorrido",
        startedAt = Instant.fromEpochSeconds(1_700_000_000),
        goal = RunGoal.FIVE_KILOMETERS,
        goalReached = goalReached,
        isDemo = isDemo,
        distanceMeters = 5000.0,
        elapsedTimeSeconds = 1800,
        averageSpeedKmh = 10.0,
        routePoints = emptyList(),
        brakes = emptyList(),
        regularityPercent = null,
        highlights = emptyList(),
        photoPaths = emptyList(),
    )

    @Test
    fun `un recorrido cumplido tiene solo la marca de meta cumplida`() {
        assertEquals(listOf(RunBadge.GOAL_REACHED), run(goalReached = true, isDemo = false).badges())
    }

    @Test
    fun `un recorrido incompleto tiene solo la marca de incompleto`() {
        assertEquals(listOf(RunBadge.INCOMPLETE), run(goalReached = false, isDemo = false).badges())
    }

    @Test
    fun `un recorrido cumplido y demo tiene ambas marcas en ese orden`() {
        assertEquals(listOf(RunBadge.GOAL_REACHED, RunBadge.DEMO), run(goalReached = true, isDemo = true).badges())
    }

    @Test
    fun `un recorrido incompleto y demo tiene ambas marcas en ese orden`() {
        assertEquals(listOf(RunBadge.INCOMPLETE, RunBadge.DEMO), run(goalReached = false, isDemo = true).badges())
    }
}
