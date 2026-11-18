package com.trazo.app.domain.model.regularity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegularityStateTest {
    @Test
    fun `8 de 10 tramos de vibracion baja da 80`() {
        val state = RegularityState(completedSegments = 10, lowVibrationSegments = 8)

        assertEquals(80, state.percentage)
    }

    @Test
    fun `sin tramos el porcentaje es null y no 0`() {
        assertNull(RegularityState().percentage)
    }

    @Test
    fun `sin sensor el porcentaje es null aunque haya tramos`() {
        val state =
            RegularityState(completedSegments = 10, lowVibrationSegments = 8, sensorAvailable = false)

        assertNull(state.percentage)
    }

    @Test
    fun `redondea al entero mas cercano`() {
        assertEquals(33, RegularityState(completedSegments = 3, lowVibrationSegments = 1).percentage)
        assertEquals(67, RegularityState(completedSegments = 3, lowVibrationSegments = 2).percentage)
        assertEquals(13, RegularityState(completedSegments = 8, lowVibrationSegments = 1).percentage)
    }

    @Test
    fun `todos los tramos de vibracion baja da 100 y ninguno da 0`() {
        assertEquals(100, RegularityState(completedSegments = 4, lowVibrationSegments = 4).percentage)
        assertEquals(0, RegularityState(completedSegments = 4, lowVibrationSegments = 0).percentage)
    }
}
