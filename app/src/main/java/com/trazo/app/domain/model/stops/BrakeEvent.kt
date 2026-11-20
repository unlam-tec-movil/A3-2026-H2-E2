package com.trazo.app.domain.model.stops

/** Frenada del recorrido [runId]: dónde y cuándo el corredor se detuvo 8 s. */
data class BrakeEvent(
    val id: String,
    val runId: String,
    val location: StopLocation?,
    val detectedAtMillis: Long,
)
