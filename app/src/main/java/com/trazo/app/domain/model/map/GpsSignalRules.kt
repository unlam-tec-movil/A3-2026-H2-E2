package com.trazo.app.domain.model.map

/** Reglas de la señal GPS: qué punto es preciso y cuándo se considera perdida la señal. */
object GpsSignalRules {
    const val MAX_ACCURACY_METERS = 30f
    const val SIGNAL_LOSS_TIMEOUT_MILLIS = 15_000L

    /** Una precisión de hasta [MAX_ACCURACY_METERS] metros se acepta. */
    fun isAccuracyAcceptable(accuracyMeters: Float): Boolean = accuracyMeters <= MAX_ACCURACY_METERS

    /**
     * La señal se pierde tras [SIGNAL_LOSS_TIMEOUT_MILLIS] sin una ubicación válida. Sin referencia
     * (`<= 0`) no se considera perdida.
     */
    fun isSignalLost(
        lastValidLocationMonotonicTimestamp: Long,
        currentMonotonicTimestamp: Long,
    ): Boolean {
        if (lastValidLocationMonotonicTimestamp <= 0L) return false

        return currentMonotonicTimestamp - lastValidLocationMonotonicTimestamp >= SIGNAL_LOSS_TIMEOUT_MILLIS
    }
}

/** El punto es lo bastante preciso para sumarse a la traza. */
fun RoutePoint.isAccurate(): Boolean = GpsSignalRules.isAccuracyAcceptable(accuracyMeters)
