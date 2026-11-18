package com.trazo.app.domain.model.regularity

/** Parámetros calibrables de la medición de regularidad del piso. */
data class RegularityConfig(
    val segmentMillis: Long = 10_000,
    /**
     * Desvío estándar de la magnitud (m/s²) por debajo del cual un tramo cuenta como vibración baja.
     * PROVISIONAL: sin respaldo; se calibra en dispositivo (tarea 5.1).
     */
    val lowVibrationStdDevThreshold: Float = 2.0f,
    /** Hueco máximo entre muestras consecutivas; si se supera, el tramo abierto se descarta. */
    val maxSampleGapMillis: Long = 2_000,
)
