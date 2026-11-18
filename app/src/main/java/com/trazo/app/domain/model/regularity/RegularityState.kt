package com.trazo.app.domain.model.regularity

/** Regularidad del piso del recorrido: tramos de 10 s cerrados y cuántos fueron de vibración baja. */
data class RegularityState(
    val runId: String? = null,
    val completedSegments: Int = 0,
    val lowVibrationSegments: Int = 0,
    val sensorAvailable: Boolean = true,
) {
    /** Porcentaje entero 0..100 redondeado al más cercano; `null` si no hay tramos o no hay sensor. */
    val percentage: Int?
        get() =
            if (!sensorAvailable || completedSegments == 0) {
                null
            } else {
                (lowVibrationSegments * 100 + completedSegments / 2) / completedSegments
            }
}
