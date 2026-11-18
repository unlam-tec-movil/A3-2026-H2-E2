package com.trazo.app.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReduceMotionTest {
    @Test
    fun `con la escala en cero las animaciones estan desactivadas`() {
        assertTrue(isReduceMotionScale(0f))
    }

    @Test
    fun `con la escala normal las animaciones siguen activas`() {
        assertFalse(isReduceMotionScale(1f))
    }

    @Test
    fun `con una escala distinta de cero las animaciones siguen activas`() {
        assertFalse(isReduceMotionScale(0.5f))
        assertFalse(isReduceMotionScale(2f))
    }
}
