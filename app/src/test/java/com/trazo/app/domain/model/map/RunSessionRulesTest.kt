package com.trazo.app.domain.model.map

import com.trazo.app.domain.model.stops.BrakeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

/** Reglas de [RunSession.ActiveSession]: transiciones, tiempo acumulado, distancia y objetivo. */
class RunSessionRulesTest {
    private val initialized =
        InitializedRunSession(
            id = "r1",
            goal = RunGoal.THREE_KILOMETERS,
            startMonotonicTimestamp = 1_000L,
            startedAt = Instant.fromEpochMilliseconds(0L),
        )

    /** Sesión activa con el primer tramo abierto en t = 1 s. */
    private val started = RunSession.ActiveSession.start(initialized, nowMillis = 1_000L)

    private fun point(
        latitude: Double,
        atMillis: Long,
        speed: Float = 3f,
    ) = RoutePoint(latitude, -58.4, accuracyMeters = 5f, speedMetersPerSecond = speed, monotonicTimestampMillis = atMillis)

    @Test
    fun `al iniciar queda activa, sin puntos y con senal debil`() {
        assertEquals(RunState.ACTIVE, started.state)
        assertEquals(GpsQuality.WEAK_SIGNAL, started.gpsQuality)
        assertEquals(1_000L, started.resumedAtMonotonicTimestamp)
        assertTrue(started.routePoints.isEmpty())
        assertFalse(started.goalReached)
    }

    @Test
    fun `pausar congela el tiempo y cierra el tramo`() {
        val paused = started.pause(nowMillis = 11_000L)

        assertEquals(RunState.PAUSED, paused.state)
        assertEquals(10_000L, paused.accumulatedElapsedTimeMillis)
        assertEquals(10L, paused.metrics.elapsedTimeSeconds)
        assertNull(paused.resumedAtMonotonicTimestamp)
        assertEquals(10_000L, paused.elapsedAt(nowMillis = 60_000L))
    }

    @Test
    fun `el tiempo acumulado no cuenta las pausas pero si las frenadas`() {
        val session =
            started
                .pause(nowMillis = 11_000L)
                .resume(nowMillis = 41_000L)
                .markStopped(nowMillis = 46_000L)
                .markMoving(nowMillis = 50_000L)

        // 10 s antes de la pausa, 5 s activo, 4 s de frenada (D3) y 2 s activo.
        assertEquals(10_000L + 5_000L + 4_000L + 2_000L, session.elapsedAt(nowMillis = 52_000L))
    }

    @Test
    fun `una frenada deja el tramo abierto y sigue registrando (D3)`() {
        val stopped = started.markStopped(nowMillis = 6_000L)

        assertEquals(RunState.STOPPED, stopped.state)
        assertTrue(stopped.isTracking)
        assertEquals(1_000L, stopped.resumedAtMonotonicTimestamp)
        assertEquals(5L, stopped.metrics.elapsedTimeSeconds)
        assertEquals(10L, stopped.tick(nowMillis = 11_000L, signalLost = false).metrics.elapsedTimeSeconds)
        assertEquals(GpsQuality.FULL_SIGNAL, stopped.withGpsQuality(GpsQuality.FULL_SIGNAL, nowMillis = 2_000L).gpsQuality)
        val withPoint = stopped.addPoint(point(-34.6, atMillis = 7_000L), nowMillis = 7_000L)
        assertEquals(1, withPoint.routePoints.size)
    }

    @Test
    fun `volver a moverse desde una frenada no reinicia el tramo ni la senal`() {
        val withPoint = started.withGpsQuality(GpsQuality.FULL_SIGNAL, nowMillis = 1_000L).addPoint(point(-34.6, atMillis = 2_000L), 2_000L)

        val moving = withPoint.markStopped(nowMillis = 4_000L).markMoving(nowMillis = 9_000L)

        assertEquals(RunState.ACTIVE, moving.state)
        assertEquals(1_000L, moving.resumedAtMonotonicTimestamp)
        assertEquals(2_000L, moving.lastValidLocationTimestamp)
        assertEquals(GpsQuality.FULL_SIGNAL, moving.gpsQuality)
        assertEquals(8L, moving.metrics.elapsedTimeSeconds)
    }

    @Test
    fun `pausa personal desde una frenada congela el tiempo con la frenada incluida (D1)`() {
        val paused = started.markStopped(nowMillis = 6_000L).pausePersonally(nowMillis = 9_000L)

        assertEquals(RunState.PERSONAL_PAUSED, paused.state)
        assertFalse(paused.isTracking)
        assertNull(paused.resumedAtMonotonicTimestamp)
        assertEquals(8_000L, paused.elapsedAt(nowMillis = 60_000L))
    }

    @Test
    fun `volver a moverse desde la pausa personal abre un tramo nuevo (D2)`() {
        val moving = started.pausePersonally(nowMillis = 3_000L).markMoving(nowMillis = 10_000L)

        assertEquals(RunState.ACTIVE, moving.state)
        assertEquals(10_000L, moving.resumedAtMonotonicTimestamp)
        assertEquals(GpsQuality.WEAK_SIGNAL, moving.gpsQuality)
        assertEquals(2_000L + 5_000L, moving.elapsedAt(nowMillis = 15_000L))
    }

    @Test
    fun `la pausa manual tambien se puede desde una frenada`() {
        val paused = started.markStopped(nowMillis = 6_000L).pause(nowMillis = 8_000L)

        assertEquals(RunState.PAUSED, paused.state)
        assertEquals(7_000L, paused.elapsedAt(nowMillis = 60_000L))
    }

    @Test
    fun `reanudar abre un tramo nuevo con senal debil y sin ultimo punto`() {
        val resumed =
            started
                .addPoint(point(-34.6, atMillis = 2_000L), nowMillis = 2_000L)
                .pausePersonally(nowMillis = 3_000L)
                .resume(nowMillis = 9_000L)

        assertEquals(RunState.ACTIVE, resumed.state)
        assertEquals(9_000L, resumed.resumedAtMonotonicTimestamp)
        assertNull(resumed.lastValidLocationTimestamp)
        assertEquals(GpsQuality.WEAK_SIGNAL, resumed.gpsQuality)
    }

    @Test
    fun `cada transicion pasa al estado esperado`() {
        assertEquals(RunState.PERSONAL_PAUSED, started.pausePersonally(2_000L).state)
        assertEquals(RunState.STOPPED, started.markStopped(2_000L).state)
        assertEquals(RunState.ACTIVE, started.markStopped(2_000L).markMoving(3_000L).state)
        assertEquals(RunState.ACTIVE, started.pausePersonally(2_000L).resume(3_000L).state)
        assertEquals(RunState.PERSONAL_PAUSED, started.markStopped(2_000L).pausePersonally(3_000L).state)
        assertEquals(RunState.ACTIVE, started.pausePersonally(2_000L).markMoving(3_000L).state)
        assertEquals(RunState.PAUSED, started.markStopped(2_000L).pause(3_000L).state)
    }

    @Test
    fun `las transiciones invalidas lanzan IllegalStateException con su mensaje`() {
        val paused = started.pause(2_000L)
        val stopped = started.markStopped(2_000L)
        val personalPaused = started.pausePersonally(2_000L)

        assertEquals("El recorrido no está en curso.", assertThrows(IllegalStateException::class.java) { paused.pause(3_000L) }.message)
        assertEquals(
            "El recorrido no está en curso.",
            assertThrows(IllegalStateException::class.java) { personalPaused.pausePersonally(3_000L) }.message,
        )
        assertEquals("El recorrido no está activo.", assertThrows(IllegalStateException::class.java) { paused.markStopped(3_000L) }.message)
        assertEquals(
            "El recorrido no está activo.",
            assertThrows(IllegalStateException::class.java) { stopped.markStopped(3_000L) }.message,
        )
        // D2: markMoving no reanuda una pausa manual ni actúa sobre un recorrido activo.
        listOf(started, paused).forEach { session ->
            assertEquals(
                "La sesión no está detenida ni en pausa personal.",
                assertThrows(IllegalStateException::class.java) { session.markMoving(3_000L) }.message,
            )
        }
        assertEquals(
            "El recorrido no está pausado manualmente.",
            assertThrows(IllegalStateException::class.java) { stopped.resume(3_000L) }.message,
        )
    }

    @Test
    fun `sumar puntos acumula distancia, velocidad y ultimo punto valido`() {
        val first = point(-34.6, atMillis = 2_000L)
        val second = point(-34.59, atMillis = 3_000L, speed = 5f)

        val session = started.addPoint(first, nowMillis = 2_000L).addPoint(second, nowMillis = 3_601_000L)

        assertEquals(listOf(first, second), session.routePoints)
        assertEquals(first.distanceMetersTo(second), session.metrics.distanceMeters, 1e-9)
        assertEquals(18.0, session.metrics.currentSpeedKmh, 1e-6)
        assertEquals(3_000L, session.lastValidLocationTimestamp)
        // 1 h de recorrido: la velocidad promedio en km/h es la distancia en km.
        assertEquals(session.metrics.distanceMeters / 1_000.0, session.metrics.averageSpeedKmh, 1e-9)
    }

    @Test
    fun `una velocidad negativa del GPS cuenta como cero`() {
        val session = started.addPoint(point(-34.6, atMillis = 2_000L, speed = -1f), nowMillis = 2_000L)

        assertEquals(0.0, session.metrics.currentSpeedKmh, 0.0)
    }

    @Test
    fun `en pausa los puntos, el reloj y la senal no cambian la sesion`() {
        listOf(started.pause(2_000L), started.pausePersonally(2_000L)).forEach { paused ->
            assertSame(paused, paused.addPoint(point(-34.6, atMillis = 3_000L), nowMillis = 3_000L))
            assertSame(paused, paused.tick(nowMillis = 30_000L, signalLost = true))
            assertSame(paused, paused.withGpsQuality(GpsQuality.FULL_SIGNAL, nowMillis = 2_000L))
        }
    }

    @Test
    fun `el objetivo se cumple al llegar a la distancia y no se pierde`() {
        // ~0,0273 grados de latitud son ~3,03 km.
        val reached =
            started
                .addPoint(point(0.0, atMillis = 2_000L), nowMillis = 2_000L)
                .addPoint(point(0.0273, atMillis = 3_000L), nowMillis = 3_000L)

        assertTrue(reached.goalReached)
        assertTrue(reached.addPoint(point(0.0273, atMillis = 4_000L), nowMillis = 4_000L).goalReached)
        assertFalse(started.addPoint(point(0.0, atMillis = 2_000L), 2_000L).addPoint(point(0.02, 3_000L), 3_000L).goalReached)
    }

    @Test
    fun `el tick actualiza el tiempo y marca sin senal solo si se perdio`() {
        val ticked = started.withGpsQuality(GpsQuality.FULL_SIGNAL, nowMillis = 1_000L).tick(nowMillis = 6_000L, signalLost = false)
        val lost = ticked.tick(nowMillis = 21_000L, signalLost = true)

        assertEquals(5L, ticked.metrics.elapsedTimeSeconds)
        assertEquals(GpsQuality.FULL_SIGNAL, ticked.gpsQuality)
        assertEquals(GpsQuality.NO_SIGNAL, lost.gpsQuality)
        assertEquals(20L, lost.metrics.elapsedTimeSeconds)
    }

    @Test
    fun `la referencia de senal es el ultimo punto valido, si no la ultima reanudacion`() {
        assertEquals(1_000L, started.signalReferenceAt(nowMillis = 5_000L))
        assertEquals(2_500L, started.addPoint(point(-34.6, atMillis = 2_500L), 3_000L).signalReferenceAt(5_000L))
        assertEquals(5_000L, started.pause(2_000L).signalReferenceAt(5_000L))
    }

    @Test
    fun `el resumen final cierra las metricas en el instante de terminar`() {
        val session = started.addPoint(point(-34.6, atMillis = 2_000L), nowMillis = 2_000L)

        val summary = session.toFrozenSummary(nowMillis = 61_000L)

        assertEquals("r1", summary.id)
        assertEquals(RunGoal.THREE_KILOMETERS, summary.goal)
        assertEquals(session.routePoints, summary.routePoints)
        assertEquals(60L, summary.metrics.elapsedTimeSeconds)
        assertEquals(1_000L, summary.startedAtMonotonicTimestamp)
        assertEquals(61_000L, summary.finishedAtMonotonicTimestamp)
        assertFalse(summary.goalReached)
        assertEquals(0, summary.brakeCount)
        assertNull(summary.regularityPercent)
    }

    @Test
    fun `el resumen final lleva las frenadas y la regularidad que recibe`() {
        val brakes =
            listOf("b1", "b2").map { BrakeEvent(id = it, runId = "r1", location = null, detectedAtMillis = 0L) }

        val summary = started.toFrozenSummary(nowMillis = 61_000L, brakes = brakes, regularityPercent = 80)

        assertEquals(brakes, summary.brakes)
        assertEquals(2, summary.brakeCount)
        assertEquals(80, summary.regularityPercent)
    }

    @Test
    fun `la velocidad promedio es cero sin tiempo transcurrido`() {
        assertEquals(0.0, RunSession.ActiveSession.averageSpeedKmh(1_000.0, 0L), 0.0)
        assertEquals(12.0, RunSession.ActiveSession.averageSpeedKmh(1_000.0, 300_000L), 1e-9)
    }

    @Test
    fun `con la senal perdida un punto impreciso no la pasa a debil`() {
        // Arranca en 1 s sin puntos: a los 16 s ya pasaron los 15 s sin señal.
        val lost = started.tick(nowMillis = 16_000L, signalLost = started.isSignalLostAt(16_000L))
        assertEquals(GpsQuality.NO_SIGNAL, lost.gpsQuality)

        assertEquals(GpsQuality.NO_SIGNAL, lost.withGpsQuality(GpsQuality.WEAK_SIGNAL, nowMillis = 17_000L).gpsQuality)
    }

    @Test
    fun `con un punto valido reciente la calidad informada se aplica`() {
        val withPoint = started.addPoint(point(-34.6, atMillis = 20_000L), nowMillis = 20_000L)

        assertEquals(GpsQuality.WEAK_SIGNAL, withPoint.withGpsQuality(GpsQuality.WEAK_SIGNAL, nowMillis = 21_000L).gpsQuality)
    }
}
