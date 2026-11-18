package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.stops.MotionSample
import com.trazo.app.domain.model.stops.StopDetectionConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StillnessEstimatorTest {
    private val config = StopDetectionConfig()

    private fun feed(
        estimator: StillnessEstimator,
        durationMillis: Long,
        stepMillis: Long = 62,
        magnitude: (Int) -> Float,
    ): Boolean {
        var result = false
        var i = 0
        var t = 0L
        while (t <= durationMillis) {
            result = estimator.accept(MotionSample(t, magnitude(i)))
            i++
            t += stepMillis
        }
        return result
    }

    @Test
    fun `serie constante durante la ventana completa es quieto`() {
        val estimator = StillnessEstimator(config)

        assertTrue(feed(estimator, 2_000) { 9.81f })
    }

    @Test
    fun `desviacion mayor al umbral es en movimiento`() {
        val estimator = StillnessEstimator(config)

        assertFalse(feed(estimator, 2_000) { i -> if (i % 2 == 0) 6f else 13f })
    }

    @Test
    fun `ventana de menos de 1000 ms es en movimiento`() {
        val estimator = StillnessEstimator(config)

        assertFalse(feed(estimator, 900) { 9.81f })
    }

    @Test
    fun `el movimiento pasado sale de la ventana y vuelve a quieto`() {
        val estimator = StillnessEstimator(config)
        feed(estimator, 1_000) { i -> if (i % 2 == 0) 6f else 13f }

        var last = false
        var t = 1_062L
        while (t <= 4_000L) {
            last = estimator.accept(MotionSample(t, 9.81f))
            t += 62
        }

        assertTrue(last)
    }

    @Test
    fun `reset descarta la ventana`() {
        val estimator = StillnessEstimator(config)
        feed(estimator, 2_000) { 9.81f }
        estimator.reset()

        assertFalse(estimator.accept(MotionSample(3_000, 9.81f)))
    }
}
