package com.trazo.app.domain.services

import com.trazo.app.domain.model.regularity.RegularityConfig
import com.trazo.app.domain.model.regularity.SegmentClass
import kotlin.math.sqrt

/**
 * Reductor puro: agrupa muestras en tramos consecutivos y clasifica cada tramo cerrado por el desvío
 * estándar de la magnitud. Sin reloj ni E/S: recibe el tiempo en cada muestra (`nowMillis` no decrece).
 */
class RegularityMeter(
    private val config: RegularityConfig,
) {
    private var open = false
    private var startMillis = 0L
    private var lastMillis = 0L
    private var count = 0
    private var mean = 0.0
    private var m2 = 0.0

    /** Devuelve la clase del tramo si esta muestra lo cierra; `null` si sigue abierto. */
    fun accept(
        nowMillis: Long,
        magnitude: Float,
    ): SegmentClass? {
        if (open && nowMillis - lastMillis > config.maxSampleGapMillis) reset()
        if (!open) begin(nowMillis)
        lastMillis = nowMillis
        add(magnitude.toDouble())
        if (nowMillis - startMillis < config.segmentMillis) return null

        val stdDev = sqrt(m2 / count)
        reset()
        return if (stdDev < config.lowVibrationStdDevThreshold) {
            SegmentClass.LOW_VIBRATION
        } else {
            SegmentClass.HIGH_VIBRATION
        }
    }

    /** Descarta el tramo abierto. */
    fun reset() {
        open = false
        count = 0
        mean = 0.0
        m2 = 0.0
    }

    private fun begin(nowMillis: Long) {
        open = true
        startMillis = nowMillis
    }

    private fun add(value: Double) {
        count++
        val delta = value - mean
        mean += delta / count
        m2 += delta * (value - mean)
    }
}
