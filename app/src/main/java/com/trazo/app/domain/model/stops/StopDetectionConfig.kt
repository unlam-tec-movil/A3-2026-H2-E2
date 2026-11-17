package com.trazo.app.domain.model.stops

/** Umbrales y tiempos calibrables de la detección de detenciones. */
data class StopDetectionConfig(
    val stillStdDevThreshold: Float = 0.5f,
    val windowMillis: Long = 1_000,
    val brakeAfterMillis: Long = 8_000,
    val classifyAfterMillis: Long = 20_000,
    val resumeConfirmMillis: Long = 1_500,
    val tickIntervalMillis: Long = 500,
    val sampleStaleMillis: Long = 2_000,
)
