package com.trazo.app.ui.screens.map.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.ui.components.layout.rememberTrazoMapState
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ovh.plrapps.mapcompose.api.hasPath
import ovh.plrapps.mapcompose.ui.state.MapState

/** Dibujo de las trazas guardadas sobre el mapa (profile-runs, requisitos 5.1, 5.3, 5.5 y 5.6). */
@RunWith(AndroidJUnit4::class)
class MapLayerSavedTracesTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mapState: MapState
    private val drawn = mutableMapOf<String, List<RoutePoint>>()

    private fun point(index: Int) =
        RoutePoint(
            latitude = -34.6 + index * 0.001,
            longitude = -58.4,
            accuracyMeters = 5f,
            speedMetersPerSecond = 2f,
            monotonicTimestampMillis = index.toLong(),
        )

    private fun trace(size: Int = 3) = List(size) { point(it) }

    private fun showMapState() {
        composeTestRule.setContent {
            TrazoTheme { mapState = rememberTrazoMapState() }
        }
        composeTestRule.waitForIdle()
    }

    private fun sync(traces: Map<String, List<RoutePoint>>) {
        composeTestRule.runOnIdle {
            mapState.syncSavedTraces(traces, Color.Red, 4.dp, drawn)
        }
    }

    private fun hasTrace(id: String) = composeTestRule.runOnIdle { mapState.hasPath(savedTracePathId(id)) }

    @Test
    fun `cada recorrido se dibuja con su propio path`() {
        showMapState()

        sync(mapOf("a" to trace(), "b" to trace()))

        assertTrue(hasTrace("a"))
        assertTrue(hasTrace("b"))
        assertEquals(setOf("a", "b"), drawn.keys)
    }

    @Test
    fun `al agregarse una traza nueva se suma sin quitar las que estaban`() {
        showMapState()
        sync(mapOf("a" to trace()))

        sync(mapOf("a" to trace(), "nuevo" to trace()))

        assertTrue(hasTrace("a"))
        assertTrue(hasTrace("nuevo"))
    }

    @Test
    fun `las trazas que ya no estan se quitan`() {
        showMapState()
        sync(mapOf("a" to trace(), "b" to trace()))

        sync(mapOf("b" to trace()))

        assertFalse(hasTrace("a"))
        assertTrue(hasTrace("b"))
    }

    @Test
    fun `con la lista vacia se quitan todas`() {
        showMapState()
        sync(mapOf("a" to trace(), "b" to trace()))

        sync(emptyMap())

        assertFalse(hasTrace("a"))
        assertFalse(hasTrace("b"))
        assertTrue(drawn.isEmpty())
    }

    @Test
    fun `una traza que cambia de puntos se actualiza y sigue dibujada`() {
        showMapState()
        sync(mapOf("a" to trace(2)))

        sync(mapOf("a" to trace(4)))

        assertTrue(hasTrace("a"))
        assertEquals(trace(4), drawn["a"])
    }

    @Test
    fun `una traza de menos de 2 puntos no se dibuja`() {
        showMapState()

        sync(mapOf("corta" to trace(1), "vacia" to emptyList(), "ok" to trace()))

        assertFalse(hasTrace("corta"))
        assertFalse(hasTrace("vacia"))
        assertTrue(hasTrace("ok"))
    }

    @Test
    fun `una traza que queda con menos de 2 puntos se quita`() {
        showMapState()
        sync(mapOf("a" to trace()))

        sync(mapOf("a" to trace(1)))

        assertFalse(hasTrace("a"))
    }

    @Test
    fun `las trazas guardadas no usan los paths del trazo en vivo`() {
        showMapState()

        sync(mapOf("route" to trace()))

        assertTrue(hasTrace("route"))
        assertFalse(composeTestRule.runOnIdle { mapState.hasPath("route") })
    }
}
