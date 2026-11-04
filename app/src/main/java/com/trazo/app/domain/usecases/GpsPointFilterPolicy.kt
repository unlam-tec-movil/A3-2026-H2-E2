package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.map.RoutePoint

class GpsPointFilterPolicy(
    private val maxAllowedAccuracyMeters: Float = 30f,
    private val signalLossTimeoutMillis: Long = 15_000L,
) {
    fun isValid(point: RoutePoint): Boolean =
        point.accuracyMeters in 0f..maxAllowedAccuracyMeters &&
            point.latitude in -90.0..90.0 &&
            point.longitude in -180.0..180.0

    fun isSignalLost(
        lastValidTimestampMillis: Long?,
        currentTimestampMillis: Long,
    ): Boolean =
        lastValidTimestampMillis != null && currentTimestampMillis - lastValidTimestampMillis >=
            signalLossTimeoutMillis
}
