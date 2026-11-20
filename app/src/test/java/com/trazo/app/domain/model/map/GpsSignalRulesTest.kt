package com.trazo.app.domain.model.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class GpsSignalRulesTest {
    private fun pointWithAccuracy(accuracyMeters: Float) =
        RoutePoint(-34.6, -58.4, accuracyMeters, speedMetersPerSecond = 0f, monotonicTimestampMillis = 0L)

    @Test
    fun `isAccurate acepta precision de hasta 30 metros`() {
        assertTrue(pointWithAccuracy(30f).isAccurate())
    }

    @Test
    fun `isAccurate rechaza precision superior a 30 metros`() {
        assertFalse(pointWithAccuracy(30.1f).isAccurate())
    }

    @Test
    fun `isSignalLost devuelve false antes de los 15 segundos`() {
        val result =
            GpsSignalRules.isSignalLost(
                lastValidLocationMonotonicTimestamp = 1_000L,
                currentMonotonicTimestamp = 15_999L,
            )

        assertFalse(result)
    }

    @Test
    fun `isSignalLost devuelve true al cumplir los 15 segundos`() {
        val result =
            GpsSignalRules.isSignalLost(
                lastValidLocationMonotonicTimestamp = 1_000L,
                currentMonotonicTimestamp = 16_000L,
            )

        assertTrue(result)
    }

    @Test
    fun `isSignalLost devuelve false cuando ultima ubicacion es menor o igual a cero`() {
        val result =
            GpsSignalRules.isSignalLost(
                lastValidLocationMonotonicTimestamp = 0L,
                currentMonotonicTimestamp = 20_000L,
            )

        assertFalse(result)
    }

    @Test
    fun `la sesion pierde la senal 15 s despues del ultimo punto valido`() {
        val session =
            RunSession.ActiveSession(
                id = "r1",
                goal = RunGoal.THREE_KILOMETERS,
                state = RunState.ACTIVE,
                startMonotonicTimestamp = 0L,
                startedAt = Instant.fromEpochMilliseconds(0L),
                lastValidLocationTimestamp = 1_000L,
            )

        assertFalse(session.isSignalLostAt(15_999L))
        assertTrue(session.isSignalLostAt(16_000L))
    }
}
