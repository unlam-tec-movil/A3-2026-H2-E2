package com.trazo.app.domain.services

import com.trazo.app.domain.model.stops.MotionSample
import com.trazo.app.domain.model.stops.StopDetectionConfig
import kotlin.math.sqrt

/**
 * Decide «quieto» con la desviación estándar de la magnitud de la aceleración en una ventana
 * deslizante. La gravedad es un offset constante, así que no depende de la orientación del teléfono.
 */
class StillnessEstimator(
    private val config: StopDetectionConfig,
) {
    private val window = ArrayDeque<MotionSample>()

    /** Devuelve true si el corredor está quieto. Con la ventana incompleta devuelve false. */
    fun accept(sample: MotionSample): Boolean {
        window.addLast(sample)
        val cutoff = sample.timestampMillis - config.windowMillis
        // Descarta la muestra más vieja solo si la siguiente ya cubre toda la ventana.
        while (window.size > 1 && window[1].timestampMillis <= cutoff) window.removeFirst()

        val coversWindow = sample.timestampMillis - window.first().timestampMillis >= config.windowMillis
        if (!coversWindow) return false

        val mean = window.sumOf { it.magnitude.toDouble() } / window.size
        val variance = window.sumOf { (it.magnitude - mean) * (it.magnitude - mean) } / window.size
        return sqrt(variance) < config.stillStdDevThreshold
    }

    fun reset() = window.clear()
}
