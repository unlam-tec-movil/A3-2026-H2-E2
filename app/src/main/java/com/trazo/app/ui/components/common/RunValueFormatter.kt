package com.trazo.app.ui.components.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Instant

/** Formato `es-AR` de los valores de un recorrido guardado. Funciones puras, compartidas por la card y el detalle. */
object RunValueFormatter {
    private val locale: Locale = Locale.forLanguageTag("es-AR")
    private const val METERS_PER_KILOMETER = 1000.0
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3600L
    private const val NO_DATA = "–"

    /** `5200.0` -> `5,2 km`. Siempre con un decimal. */
    fun formatDistanceKm(distanceMeters: Double): String = String.format(locale, "%.1f km", distanceMeters / METERS_PER_KILOMETER)

    /** `3300` -> `00:55:00`. */
    fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / SECONDS_PER_HOUR
        val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return String.format(locale, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    /** `9.5` -> `9,5 km/h`. */
    fun formatAverageSpeedKmh(speedKmh: Double): String = String.format(locale, "%.1f km/h", speedKmh)

    /** `80` -> `80 %`; sin dato (`null`) -> `–`. */
    fun formatRegularity(regularityPercent: Int?): String = regularityPercent?.let { String.format(locale, "%d %%", it) } ?: NO_DATA

    /** `Mar 28 oct`: día y mes abreviados, primera letra en mayúscula y sin puntos de abreviatura. */
    fun formatShortDate(
        instant: Instant,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String {
        // SimpleDateFormat y no java.time: minSdk 24 sin desugaring.
        val format = SimpleDateFormat("EEE d MMM", locale).apply { this.timeZone = timeZone }
        val raw = format.format(Date(instant.toEpochMilliseconds())).replace(".", "")
        return raw.replaceFirstChar { it.titlecase(locale) }
    }
}
