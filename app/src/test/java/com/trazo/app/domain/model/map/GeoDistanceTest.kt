package com.trazo.app.domain.model.map

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoDistanceTest {
    private fun point(
        latitude: Double,
        longitude: Double,
    ) = RoutePoint(latitude, longitude, accuracyMeters = 5f, speedMetersPerSecond = 0f, monotonicTimestampMillis = 0L)

    @Test
    fun `el mismo punto esta a cero metros`() {
        assertEquals(0.0, point(-34.6, -58.4).distanceMetersTo(point(-34.6, -58.4)), 0.0)
    }

    @Test
    fun `un grado de latitud mide unos 111 km`() {
        assertEquals(111_194.93, point(0.0, 0.0).distanceMetersTo(point(1.0, 0.0)), 0.01)
    }

    @Test
    fun `la distancia es la misma en ambos sentidos`() {
        val obelisco = point(-34.6037, -58.3816)
        val congreso = point(-34.6098, -58.3925)

        assertEquals(obelisco.distanceMetersTo(congreso), congreso.distanceMetersTo(obelisco), 1e-9)
        assertEquals(1_206.3, obelisco.distanceMetersTo(congreso), 0.1)
    }
}
