package com.trazo.app.ui.components.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone
import kotlin.time.Instant

class RunValueFormatterTest {
    @Test
    fun `distancia con decimales usa coma`() {
        assertEquals("5,2 km", RunValueFormatter.formatDistanceKm(5200.0))
    }

    @Test
    fun `distancia entera lleva un decimal`() {
        assertEquals("5,0 km", RunValueFormatter.formatDistanceKm(5000.0))
    }

    @Test
    fun `distancia cero`() {
        assertEquals("0,0 km", RunValueFormatter.formatDistanceKm(0.0))
    }

    @Test
    fun `distancia se redondea a un decimal`() {
        assertEquals("8,3 km", RunValueFormatter.formatDistanceKm(8270.0))
    }

    @Test
    fun `duracion en horas minutos y segundos`() {
        assertEquals("00:00:00", RunValueFormatter.formatDuration(0))
        assertEquals("00:55:00", RunValueFormatter.formatDuration(3300))
        assertEquals("01:02:03", RunValueFormatter.formatDuration(3723))
    }

    @Test
    fun `velocidad promedio con coma y unidad`() {
        assertEquals("9,5 km/h", RunValueFormatter.formatAverageSpeedKmh(9.5))
        assertEquals("10,0 km/h", RunValueFormatter.formatAverageSpeedKmh(10.0))
        assertEquals("0,0 km/h", RunValueFormatter.formatAverageSpeedKmh(0.0))
    }

    @Test
    fun `regularidad con porcentaje`() {
        assertEquals("80 %", RunValueFormatter.formatRegularity(80))
        assertEquals("0 %", RunValueFormatter.formatRegularity(0))
        assertEquals("100 %", RunValueFormatter.formatRegularity(100))
    }

    @Test
    fun `regularidad nula se muestra como guion`() {
        assertEquals("–", RunValueFormatter.formatRegularity(null))
    }

    @Test
    fun `fecha corta con inicial mayuscula y sin puntos`() {
        // 2025-10-28 es martes.
        val instant = Instant.parse("2025-10-28T15:00:00Z")

        assertEquals("Mar 28 oct", RunValueFormatter.formatShortDate(instant, TimeZone.getTimeZone("UTC")))
    }

    @Test
    fun `fecha corta respeta la zona horaria`() {
        // 02:00 UTC del miércoles 29 es todavía martes 28 en Buenos Aires (UTC-3).
        val instant = Instant.parse("2025-10-29T02:00:00Z")

        assertEquals(
            "Mar 28 oct",
            RunValueFormatter.formatShortDate(instant, TimeZone.getTimeZone("America/Argentina/Buenos_Aires")),
        )
    }

    @Test
    fun `fecha corta de otro mes`() {
        // 2026-01-05 es lunes.
        val instant = Instant.parse("2026-01-05T12:00:00Z")

        assertEquals("Lun 5 ene", RunValueFormatter.formatShortDate(instant, TimeZone.getTimeZone("UTC")))
    }
}
