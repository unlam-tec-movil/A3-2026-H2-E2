package com.trazo.app.ui.components.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.floor

class WebMercatorTest {
    private val delta = 1e-9

    @Test
    fun origin_mapsToCenter() {
        val point = WebMercator.normalize(latitude = 0.0, longitude = 0.0)

        assertEquals(0.5, point.x, delta)
        assertEquals(0.5, point.y, delta)
    }

    @Test
    fun longitudeLimits_mapToEdges() {
        assertEquals(0.0, WebMercator.normalize(0.0, -180.0).x, delta)
        assertEquals(1.0, WebMercator.normalize(0.0, 180.0).x, delta)
    }

    @Test
    fun latitudeBeyondMercatorLimit_isClamped() {
        assertEquals(0.0, WebMercator.normalize(89.9, 0.0).y, 1e-6)
        assertEquals(1.0, WebMercator.normalize(-89.9, 0.0).y, 1e-6)
    }

    @Test
    fun buenosAires_fallsOnExpectedTileAtZoom15() {
        val point = WebMercator.normalize(latitude = -34.6037, longitude = -58.3816)
        val tilesPerSide = 1 shl 15

        assertEquals(11069, floor(point.x * tilesPerSide).toInt())
        assertEquals(19744, floor(point.y * tilesPerSide).toInt())
    }

    @Test
    fun scaleForZoom_isRelativeToMaxZoom() {
        assertEquals(1.0, WebMercator.scaleForZoom(WebMercator.MAX_ZOOM), delta)
        assertEquals(2.0, WebMercator.scaleForZoom(17), delta)
        assertEquals(0.5, WebMercator.scaleForZoom(15), delta)
    }

    @Test
    fun zoomLimits_neverOverscaleMoreThanTwice() {
        assertTrue(WebMercator.MAX_VISIBLE_ZOOM <= WebMercator.MAX_ZOOM + 1)
        assertTrue(WebMercator.MIN_VISIBLE_ZOOM < WebMercator.MAX_ZOOM)
    }

    // --- Encuadre de una traza (detalle del recorrido, requisito 3.2) ---

    private fun normalized(
        latitude: Double,
        longitude: Double,
    ) = WebMercator.normalize(latitude, longitude)

    /** Tres puntos de unos 5 km por lado en Buenos Aires: entra en un zoom intermedio, sin tocar los límites. */
    private val cityTrace =
        listOf(
            normalized(-34.60, -58.40),
            normalized(-34.64, -58.36),
            normalized(-34.62, -58.38),
        )

    @Test
    fun frameTrace_withoutPoints_returnsNull() {
        assertEquals(null, WebMercator.frameTrace(emptyList(), 1000, 800, 0))
    }

    @Test
    fun frameTrace_centersOnTheMiddleOfTheBoundingBox() {
        val framing = WebMercator.frameTrace(cityTrace, 1000, 800, 0)!!

        val xs = cityTrace.map { it.x }
        val ys = cityTrace.map { it.y }
        assertEquals((xs.min() + xs.max()) / 2, framing.centerX, delta)
        assertEquals((ys.min() + ys.max()) / 2, framing.centerY, delta)
    }

    @Test
    fun frameTrace_fitsTheWholeTraceInsideTheViewportMinusPadding() {
        val viewportWidth = 1000
        val viewportHeight = 800
        val padding = 50

        val framing = WebMercator.frameTrace(cityTrace, viewportWidth, viewportHeight, padding)!!

        val widthPx = (cityTrace.maxOf { it.x } - cityTrace.minOf { it.x }) * WebMercator.FULL_SIZE * framing.scale
        val heightPx = (cityTrace.maxOf { it.y } - cityTrace.minOf { it.y }) * WebMercator.FULL_SIZE * framing.scale
        assertTrue("ancho $widthPx", widthPx <= viewportWidth - 2 * padding + 1e-6)
        assertTrue("alto $heightPx", heightPx <= viewportHeight - 2 * padding + 1e-6)
        // El eje que manda toca el borde del recuadro: no queda más chico de lo necesario.
        assertTrue(
            widthPx >= (viewportWidth - 2 * padding) - 1e-6 || heightPx >= (viewportHeight - 2 * padding) - 1e-6,
        )
    }

    @Test
    fun frameTrace_aSinglePoint_zoomsToTheMaximum() {
        val framing = WebMercator.frameTrace(listOf(normalized(-34.6, -58.4)), 1000, 800, 50)!!

        assertEquals(WebMercator.scaleForZoom(WebMercator.MAX_VISIBLE_ZOOM), framing.scale, delta)
    }

    @Test
    fun frameTrace_aTinyTrace_neverZoomsPastTheMaximum() {
        val tiny = listOf(normalized(-34.60000, -58.40000), normalized(-34.60001, -58.40001))

        val framing = WebMercator.frameTrace(tiny, 1000, 800, 50)!!

        assertTrue(framing.scale <= WebMercator.scaleForZoom(WebMercator.MAX_VISIBLE_ZOOM) + delta)
    }

    @Test
    fun frameTrace_aHugeTrace_neverZoomsOutPastTheMinimum() {
        val huge = listOf(normalized(-50.0, -70.0), normalized(10.0, 20.0))

        val framing = WebMercator.frameTrace(huge, 1000, 800, 50)!!

        assertEquals(WebMercator.scaleForZoom(WebMercator.MIN_VISIBLE_ZOOM), framing.scale, delta)
    }

    @Test
    fun frameTrace_paddingLargerThanTheViewport_stillReturnsAValidScale() {
        val framing = WebMercator.frameTrace(cityTrace, 100, 100, 80)!!

        assertTrue(framing.scale > 0.0)
    }

    // --- Toque sobre el mapa (requisito 5.2) ---

    @Test
    fun denormalize_roundTripsNormalize() {
        listOf(-34.6037 to -58.3816, 0.0 to 0.0, 40.4168 to -3.7038, -54.8 to 68.3).forEach { (lat, lon) ->
            val point = WebMercator.normalize(lat, lon)
            val back = WebMercator.denormalize(point.x, point.y)

            assertEquals("lat $lat", lat, back.latitude, 1e-7)
            assertEquals("lon $lon", lon, back.longitude, 1e-7)
        }
    }

    @Test
    fun denormalize_centerIsTheOrigin() {
        val origin = WebMercator.denormalize(0.5, 0.5)

        assertEquals(0.0, origin.latitude, delta)
        assertEquals(0.0, origin.longitude, delta)
    }

    @Test
    fun metersPerPixel_atTheEquatorAtMaxZoom() {
        // Circunferencia terrestre / ancho del mundo en px al nivel 16 con tiles de 512 px.
        val expected = 2 * Math.PI * 6_378_137.0 / WebMercator.FULL_SIZE

        assertEquals(expected, WebMercator.metersPerPixel(0.0, 1.0), 1e-9)
    }

    @Test
    fun metersPerPixel_halvesWhenScaleDoubles() {
        val base = WebMercator.metersPerPixel(-34.6, 1.0)

        assertEquals(base / 2, WebMercator.metersPerPixel(-34.6, 2.0), 1e-12)
    }

    @Test
    fun metersPerPixel_shrinksWithLatitude() {
        assertEquals(
            WebMercator.metersPerPixel(0.0, 1.0) * kotlin.math.cos(Math.toRadians(60.0)),
            WebMercator.metersPerPixel(60.0, 1.0),
            1e-9,
        )
    }

    @Test
    fun fullSize_matchesTileGrid() {
        assertEquals(WebMercator.TILE_SIZE.toLong() shl WebMercator.MAX_ZOOM, WebMercator.FULL_SIZE.toLong())
    }
}
