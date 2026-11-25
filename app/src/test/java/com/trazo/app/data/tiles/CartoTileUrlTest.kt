package com.trazo.app.data.tiles

import org.junit.Assert.assertEquals
import org.junit.Test

class CartoTileUrlTest {
    @Test
    fun templateWithKey_replacesCoordinatesAndRetina() {
        val url =
            CartoTileUrl.build(
                template = "https://tiles.example.com/light_all/{z}/{x}/{y}{r}.png?api_key=abc",
                row = 19744,
                col = 11069,
                zoom = 15,
            )

        assertEquals("https://tiles.example.com/light_all/15/11069/19744@2x.png?api_key=abc", url)
    }

    @Test
    fun templateWithoutRetina_requestsRetinaTiles() {
        val url =
            CartoTileUrl.build(
                template = "https://basemaps.example.com/rastertiles/voyager/{z}/{x}/{y}.png?key=abc",
                row = 19744,
                col = 11069,
                zoom = 15,
            )

        assertEquals("https://basemaps.example.com/rastertiles/voyager/15/11069/19744@2x.png?key=abc", url)
    }

    @Test
    fun templateWithRetina_isNotDuplicated() {
        val url = CartoTileUrl.build("https://example.com/{z}/{x}/{y}@2x.png?key=abc", 1, 2, 3)

        assertEquals("https://example.com/3/2/1@2x.png?key=abc", url)
    }

    @Test
    fun subdomain_isDeterministic() {
        val template = "https://{s}.example.com/{z}/{x}/{y}.png"

        assertEquals("https://a.example.com/1/0/0@2x.png", CartoTileUrl.build(template, 0, 0, 1))
        assertEquals("https://d.example.com/2/1/2@2x.png", CartoTileUrl.build(template, 2, 1, 2))
    }

    @Test
    fun emptyTemplate_fallsBackToPublicUrl() {
        val url = CartoTileUrl.build(template = "  ", row = 3, col = 2, zoom = 4)

        assertEquals("https://b.basemaps.cartocdn.com/light_all/4/2/3@2x.png", url)
    }
}
