package com.trazo.app.domain.model.stops

data class BrakeEvent(
    val id: String,
    val location: StopLocation?,
    val detectedAtMillis: Long,
)
