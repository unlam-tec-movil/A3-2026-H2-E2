package com.trazo.app.ui.screens.result

import jakarta.inject.Inject
import java.util.Locale

/**
 * Da formato a los valores de la pantalla de resultado en `es-AR`.
 * Solo formatea lo que entrega el resumen congelado: no recalcula nada.
 */
class ResultFormatter
    @Inject
    constructor() {
        private val locale: Locale = Locale.forLanguageTag("es-AR")

        /** Metros a kilómetros con un decimal y coma decimal: `5200.0` -> `5,2 km`. */
        fun formatDistance(distanceMeters: Double): String = String.format(locale, "%.1f km", distanceMeters / METERS_PER_KM)

        /** Segundos a `HH:MM:SS`: `1930` -> `00:32:10`. */
        fun formatElapsedTime(elapsedSeconds: Long): String {
            val hours = elapsedSeconds / SECONDS_PER_HOUR
            val minutes = (elapsedSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
            val seconds = elapsedSeconds % SECONDS_PER_MINUTE
            return String.format(locale, "%02d:%02d:%02d", hours, minutes, seconds)
        }

        /** Regularidad con espacio y sin decimales: `80` -> `80 %`. */
        fun formatRegularity(percent: Int): String = String.format(locale, "%d %%", percent)

        private companion object {
            const val METERS_PER_KM = 1000.0
            const val SECONDS_PER_MINUTE = 60L
            const val SECONDS_PER_HOUR = 3600L
        }
    }
