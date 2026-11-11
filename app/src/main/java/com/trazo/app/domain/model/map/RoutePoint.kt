package com.trazo.app.domain.model.map

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val speedMetersPerSecond: Float,
    val monotonicTimestampMillis: Long,
)
