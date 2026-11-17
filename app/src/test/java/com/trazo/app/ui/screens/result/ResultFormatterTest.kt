package com.trazo.app.ui.screens.result

import org.junit.Assert.assertEquals
import org.junit.Test

class ResultFormatterTest {
    private val formatter = ResultFormatter()

    @Test
    fun `distance in meters is shown in km with decimal comma`() {
        assertEquals("5,2 km", formatter.formatDistance(5200.0))
    }

    @Test
    fun `distance rounds to one decimal`() {
        assertEquals("3,0 km", formatter.formatDistance(2960.0))
        assertEquals("0,0 km", formatter.formatDistance(0.0))
    }

    @Test
    fun `elapsed time is shown as HH MM SS`() {
        assertEquals("00:32:10", formatter.formatElapsedTime(1930L))
        assertEquals("00:00:00", formatter.formatElapsedTime(0L))
        assertEquals("01:01:01", formatter.formatElapsedTime(3661L))
    }

    @Test
    fun `regularity is shown with a space and no decimals`() {
        assertEquals("80 %", formatter.formatRegularity(80))
        assertEquals("0 %", formatter.formatRegularity(0))
        assertEquals("100 %", formatter.formatRegularity(100))
    }
}
