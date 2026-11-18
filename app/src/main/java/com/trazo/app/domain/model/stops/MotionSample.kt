package com.trazo.app.domain.model.stops

/** Magnitud de la aceleración en m/s² en un instante dado. */
data class MotionSample(
    val timestampMillis: Long,
    val magnitude: Float,
)
