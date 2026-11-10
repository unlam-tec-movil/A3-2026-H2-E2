package com.trazo.app.domain.repository

import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.usecases.GpsPointFilterPolicy
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class GpsPointFilterPolicyTest {
    private val policy = GpsPointFilterPolicy()

    @Test
    fun `isValid acepta precision de hasta 30 metros`() {
        val point = createPoint(accuracyMeters = 30f)

        assertTrue(policy.isValid(point))
    }

    @Test
    fun `isValid rechaza precision superior a 30 metros`() {
        val point = createPoint(accuracyMeters = 30.1f)

        assertFalse(policy.isValid(point))
    }

    @Test
    fun `isValid rechaza precision negativa`() {
        val point = createPoint(accuracyMeters = -1f)

        assertFalse(policy.isValid(point))
    }

    @Test
    fun `isValid acepta coordenadas dentro de los limites`() {
        val point =
            createPoint(
                latitude = 90.0,
                longitude = 180.0,
            )

        assertTrue(policy.isValid(point))
    }

    @Test
    fun `isValid rechaza latitud fuera de rango`() {
        val point = createPoint(latitude = 90.1)

        assertFalse(policy.isValid(point))
    }

    @Test
    fun `isValid rechaza longitud fuera de rango`() {
        val point = createPoint(longitude = -180.1)

        assertFalse(policy.isValid(point))
    }

    @Test
    fun `isSignalLost devuelve false antes de los 15 segundos`() {
        val result =
            policy.isSignalLost(
                lastValidTimestampMillis = 1_000L,
                currentTimestampMillis = 15_999L,
            )

        assertFalse(result)
    }

    @Test
    fun `isSignalLost devuelve true al cumplir los 15 segundos`() {
        val result =
            policy.isSignalLost(
                lastValidTimestampMillis = 1_000L,
                currentTimestampMillis = 16_000L,
            )

        assertTrue(result)
    }

    @Test
    fun `isSignalLost devuelve false cuando no existe ultima ubicacion`() {
        val result =
            policy.isSignalLost(
                lastValidTimestampMillis = null,
                currentTimestampMillis = 20_000L,
            )

        assertFalse(result)
    }

    private fun createPoint(
        latitude: Double = -34.6037,
        longitude: Double = -58.3816,
        accuracyMeters: Float = 10f,
    ) = RoutePoint(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters,
        speedMetersPerSecond = 0f,
        timestampMillis = 1_000L,
    )
}
