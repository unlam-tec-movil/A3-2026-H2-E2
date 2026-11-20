package com.trazo.app.ui.screens.profile

import java.util.Locale

/** Formato de los totales de «Mis estadísticas» en `es-AR`. Funciones puras: no recalculan nada. */
object ProfileStatsFormatter {
    private val locale: Locale = Locale.forLanguageTag("es-AR")
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3600L

    /** Kilómetros sin decimales si son enteros (`8.0` -> `8`) y con uno y coma si no (`8.5` -> `8,5`). */
    fun formatDistanceKm(distanceKm: Double): String {
        val rounded = String.format(locale, "%.1f", distanceKm)
        val zeroDecimal = String.format(locale, "%.1f", 0.0).substring(1)
        return rounded.removeSuffix(zeroDecimal)
    }

    /** Segundos a `HH:MM:SS`: `3300` -> `00:55:00`. */
    fun formatTotalTime(totalSeconds: Long): String {
        val hours = totalSeconds / SECONDS_PER_HOUR
        val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return String.format(locale, "%02d:%02d:%02d", hours, minutes, seconds)
    }
}
