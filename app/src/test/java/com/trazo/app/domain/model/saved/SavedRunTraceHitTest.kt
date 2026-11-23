package com.trazo.app.domain.model.saved

import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class SavedRunTraceHitTest {
    private fun point(
        latitude: Double,
        longitude: Double,
    ) = RoutePoint(latitude, longitude, 5f, 3f, 0L)

    private fun run(
        id: String,
        startedAtSeconds: Long,
        points: List<RoutePoint>,
    ) = SavedRun(
        id = id,
        title = "Recorrido $id",
        startedAt = Instant.fromEpochSeconds(startedAtSeconds),
        goal = RunGoal.FIVE_KILOMETERS,
        goalReached = true,
        isDemo = false,
        distanceMeters = 5000.0,
        elapsedTimeSeconds = 1800,
        averageSpeedKmh = 10.0,
        routePoints = points,
        brakes = emptyList(),
        regularityPercent = null,
        highlights = emptyList(),
        photoPaths = emptyList(),
    )

    // Tramo horizontal de ~1,1 km sobre el paralelo -34,6 (de lon -58,40 a -58,39).
    private val horizontal = listOf(point(-34.6, -58.40), point(-34.6, -58.39))

    // Tramo vertical que cruza al horizontal en (-34.6, -58.395).
    private val vertical = listOf(point(-34.605, -58.395), point(-34.595, -58.395))

    @Test
    fun `punto sobre la traza coincide`() {
        assertTrue(run("a", 1, horizontal).isTraceNear(-34.6, -58.395, 10.0))
    }

    @Test
    fun `punto dentro de la tolerancia coincide y fuera no`() {
        val traza = run("a", 1, horizontal)
        // 0,0001 grados de latitud son ~11 m.
        assertTrue(traza.isTraceNear(-34.6001, -58.395, 15.0))
        assertFalse(traza.isTraceNear(-34.6001, -58.395, 5.0))
    }

    @Test
    fun `punto mas alla de los extremos del tramo no coincide`() {
        assertFalse(run("a", 1, horizontal).isTraceNear(-34.6, -58.42, 24.0))
    }

    @Test
    fun `traza de un punto nunca coincide`() {
        val traza = run("a", 1, listOf(point(-34.6, -58.395)))

        assertFalse(traza.isTraceNear(-34.6, -58.395, 1000.0))
    }

    @Test
    fun `traza sin puntos nunca coincide`() {
        assertFalse(run("a", 1, emptyList()).isTraceNear(-34.6, -58.395, 1000.0))
    }

    @Test
    fun `en un cruce gana el recorrido mas reciente`() {
        val antiguo = run("antiguo", 100, horizontal)
        val reciente = run("reciente", 200, vertical)

        assertEquals("reciente", listOf(antiguo, reciente).mostRecentNear(-34.6, -58.395, 24.0)?.id)
        assertEquals("reciente", listOf(reciente, antiguo).mostRecentNear(-34.6, -58.395, 24.0)?.id)
    }

    @Test
    fun `solo coincide el recorrido que pasa cerca`() {
        val horizontalRun = run("h", 100, horizontal)
        val verticalRun = run("v", 200, vertical)

        assertEquals("h", listOf(horizontalRun, verticalRun).mostRecentNear(-34.6, -58.399, 24.0)?.id)
    }

    @Test
    fun `con empate de fecha gana el primero de la lista`() {
        val primero = run("primero", 100, horizontal)
        val segundo = run("segundo", 100, vertical)

        assertEquals("primero", listOf(primero, segundo).mostRecentNear(-34.6, -58.395, 24.0)?.id)
    }

    @Test
    fun `lista vacia o sin coincidencias devuelve null`() {
        assertNull(emptyList<SavedRun>().mostRecentNear(-34.6, -58.395, 24.0))
        assertNull(listOf(run("a", 1, horizontal)).mostRecentNear(-33.0, -58.395, 24.0))
    }
}
