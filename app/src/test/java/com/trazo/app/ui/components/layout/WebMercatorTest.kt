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

    @Test
    fun fullSize_matchesTileGrid() {
        assertEquals(WebMercator.TILE_SIZE.toLong() shl WebMercator.MAX_ZOOM, WebMercator.FULL_SIZE.toLong())
    }
}
