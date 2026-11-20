package com.trazo.app.domain.services

import javax.inject.Inject

/** Reglas de la señal GPS: qué punto es preciso y cuándo se considera perdida la señal. */
class GpsPointFilterPolicy
    @Inject
    constructor() {
        fun isPointAccurate(accuracyMeters: Float): Boolean = accuracyMeters <= MAX_ACCURACY_METERS

        fun isSignalLost(
            lastValidLocationMonotonicTimestamp: Long,
            currentMonotonicTimestamp: Long,
        ): Boolean {
            if (lastValidLocationMonotonicTimestamp <= 0L) return false

            return currentMonotonicTimestamp - lastValidLocationMonotonicTimestamp >= SIGNAL_LOSS_TIMEOUT_MILLIS
        }

        companion object {
            const val MAX_ACCURACY_METERS = 30f
            const val SIGNAL_LOSS_TIMEOUT_MILLIS = 15_000L
        }
    }
