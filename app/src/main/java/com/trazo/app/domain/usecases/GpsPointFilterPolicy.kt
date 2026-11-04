package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.map.RoutePoint
import javax.inject.Inject

class GpsPointFilterPolicy
    @Inject
    constructor() {
        fun isValid(point: RoutePoint): Boolean =
            point.accuracyMeters in 0f..MAX_ALLOWED_ACCURACY_METERS &&
                point.latitude in -90.0..90.0 &&
                point.longitude in -180.0..180.0

        fun isSignalLost(
            lastValidTimestampMillis: Long?,
            currentTimestampMillis: Long,
        ): Boolean =
            lastValidTimestampMillis != null && currentTimestampMillis - lastValidTimestampMillis >=
                SIGNAL_LOSS_TIMEOUT_MILLIS

        private companion object {
            const val MAX_ALLOWED_ACCURACY_METERS = 30f
            const val SIGNAL_LOSS_TIMEOUT_MILLIS = 15_000L
        }
    }
