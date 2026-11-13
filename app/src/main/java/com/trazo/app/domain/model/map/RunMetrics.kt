package com.trazo.app.domain.model.map

data class RunMetrics(
    val elapsedTimeSeconds: Long = 0L,
    val distanceMeters: Double = 0.0,
    val currentSpeedKmh: Double = 0.0,
    val averageSpeedKmh: Double = 0.0,
)
