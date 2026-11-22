package com.trazo.app.domain.model.saved

private const val METERS_PER_KILOMETER = 1000.0

/**
 * Totales de «Mis estadísticas». Suma los recorridos guardados completos e incompletos y deja afuera
 * los demo. El tiempo es el `elapsedTimeSeconds` de cada recorrido, que ya excluye las pausas personales.
 */
data class SavedRunTotals(
    val runCount: Int,
    val distanceKm: Double,
    val timeSeconds: Long,
) {
    companion object {
        fun from(runs: List<SavedRun>): SavedRunTotals {
            val counted = runs.filterNot { it.isDemo }
            return SavedRunTotals(
                runCount = counted.size,
                distanceKm = counted.sumOf { it.distanceMeters / METERS_PER_KILOMETER },
                timeSeconds = counted.sumOf { it.elapsedTimeSeconds },
            )
        }
    }
}
