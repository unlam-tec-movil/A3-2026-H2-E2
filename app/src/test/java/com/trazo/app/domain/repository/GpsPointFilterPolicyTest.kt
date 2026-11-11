package com.trazo.app.domain.repository

import com.trazo.app.domain.usecases.GpsPointFilterPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsPointFilterPolicyTest {
    private val policy = GpsPointFilterPolicy()

    @Test
    fun `isPointAccurate acepta precision de hasta 30 metros`() {
        assertTrue(policy.isPointAccurate(30f))
    }

    @Test
    fun `isPointAccurate rechaza precision superior a 30 metros`() {
        assertFalse(policy.isPointAccurate(30.1f))
    }

    @Test
    fun `isSignalLost devuelve false antes de los 15 segundos`() {
        val result =
            policy.isSignalLost(
                lastValidLocationMonotonicTimestamp = 1_000L,
                currentMonotonicTimestamp = 15_999L,
            )

        assertFalse(result)
    }

    @Test
    fun `isSignalLost devuelve true al cumplir los 15 segundos`() {
        val result =
            policy.isSignalLost(
                lastValidLocationMonotonicTimestamp = 1_000L,
                currentMonotonicTimestamp = 16_000L,
            )

        assertTrue(result)
    }

    @Test
    fun `isSignalLost devuelve false cuando ultima ubicacion es menor o igual a cero`() {
        val result =
            policy.isSignalLost(
                lastValidLocationMonotonicTimestamp = 0L,
                currentMonotonicTimestamp = 20_000L,
            )

        assertFalse(result)
    }
}
