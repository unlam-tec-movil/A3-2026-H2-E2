package com.trazo.app.domain.usecases

import jakarta.inject.Inject

class GpsPointFilterPolicy
    @Inject
    constructor() {
        private val maxAllowedAccuracyMeters: Float = 30.0f
        private val signalLossTimeoutMillis: Long = 15_000L

        fun isPointAccurate(accuracyMeters: Float): Boolean = accuracyMeters <= maxAllowedAccuracyMeters

        fun isSignalLost(
            lastValidLocationMonotonicTimestamp: Long,
            currentMonotonicTimestamp: Long,
        ): Boolean {
            if (lastValidLocationMonotonicTimestamp <= 0L) return false

            return currentMonotonicTimestamp - lastValidLocationMonotonicTimestamp >= signalLossTimeoutMillis
        }
    }
