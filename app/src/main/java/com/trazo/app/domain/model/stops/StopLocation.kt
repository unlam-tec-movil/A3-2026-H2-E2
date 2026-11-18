package com.trazo.app.domain.model.stops

data class StopLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
)
