package com.trazo.app.domain.model.saved

import com.trazo.app.domain.model.map.RunGoal
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

class SavedRunTotalsTest {
    private fun run(
        id: String,
        distanceMeters: Double = 5000.0,
        elapsedTimeSeconds: Long = 1800,
        goalReached: Boolean = true,
        isDemo: Boolean = false,
    ) = SavedRun(
        id = id,
        title = "Recorrido $id",
        startedAt = Instant.fromEpochSeconds(1_700_000_000),
        goal = RunGoal.FIVE_KILOMETERS,
        goalReached = goalReached,
        isDemo = isDemo,
        distanceMeters = distanceMeters,
        elapsedTimeSeconds = elapsedTimeSeconds,
        averageSpeedKmh = 10.0,
        routePoints = emptyList(),
        brakes = emptyList(),
        regularityPercent = null,
        highlights = emptyList(),
        photoPaths = emptyList(),
    )

    @Test
    fun `lista vacia devuelve cantidad distancia y tiempo en cero`() {
        assertEquals(SavedRunTotals(0, 0.0, 0L), SavedRunTotals.from(emptyList()))
    }

    @Test
    fun `solo recorridos demo devuelve ceros`() {
        val runs = listOf(run("a", isDemo = true), run("b", isDemo = true, goalReached = false))

        assertEquals(SavedRunTotals(0, 0.0, 0L), SavedRunTotals.from(runs))
    }

    @Test
    fun `los incompletos suman a los totales`() {
        val runs =
            listOf(
                run("a", distanceMeters = 1500.0, elapsedTimeSeconds = 600, goalReached = false),
                run("b", distanceMeters = 2500.0, elapsedTimeSeconds = 900, goalReached = false),
            )

        assertEquals(SavedRunTotals(2, 4.0, 1500L), SavedRunTotals.from(runs))
    }

    @Test
    fun `los demo completos e incompletos no suman y el resto si`() {
        val runs =
            listOf(
                run("real", distanceMeters = 5000.0, elapsedTimeSeconds = 1800),
                run("incompleto", distanceMeters = 1200.0, elapsedTimeSeconds = 500, goalReached = false),
                run("demo-completo", distanceMeters = 10_000.0, elapsedTimeSeconds = 3000, isDemo = true),
                run("demo-incompleto", distanceMeters = 800.0, elapsedTimeSeconds = 300, isDemo = true, goalReached = false),
            )

        val totals = SavedRunTotals.from(runs)

        assertEquals(2, totals.runCount)
        assertEquals(6.2, totals.distanceKm, 1e-9)
        assertEquals(2300L, totals.timeSeconds)
    }

    @Test
    fun `el tiempo es la suma del tiempo registrado de cada recorrido`() {
        val runs = listOf(run("a", elapsedTimeSeconds = 1234), run("b", elapsedTimeSeconds = 4321))

        assertEquals(5555L, SavedRunTotals.from(runs).timeSeconds)
    }

    @Test
    fun `el orden de la lista no cambia el resultado`() {
        val runs =
            listOf(
                run("a", distanceMeters = 3000.0, elapsedTimeSeconds = 900),
                run("b", distanceMeters = 7000.0, elapsedTimeSeconds = 2100, isDemo = true),
                run("c", distanceMeters = 1000.0, elapsedTimeSeconds = 400, goalReached = false),
            )

        assertEquals(SavedRunTotals.from(runs), SavedRunTotals.from(runs.reversed()))
    }
}
