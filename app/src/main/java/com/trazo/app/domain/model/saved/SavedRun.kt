package com.trazo.app.domain.model.saved

import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.stops.BrakeEvent
import kotlin.time.Instant

data class SavedRun(
    val id: String,
    val title: String,
    val startedAt: Instant,
    val goal: RunGoal,
    val goalReached: Boolean,
    val isDemo: Boolean,
    val distanceMeters: Double,
    val elapsedTimeSeconds: Long,
    val averageSpeedKmh: Double,
    val routePoints: List<RoutePoint>,
    val brakes: List<BrakeEvent>,
    val regularityPercent: Int?,
    val highlights: List<String>,
    val photoPaths: List<String>,
) {
    val brakeCount: Int get() = brakes.size

    /**
     * Marcas del recorrido, en orden: primero [RunBadge.GOAL_REACHED] o [RunBadge.INCOMPLETE] según
     * [goalReached], y después [RunBadge.DEMO] si [isDemo]. Nunca está vacía.
     */
    fun badges(): List<RunBadge> =
        buildList {
            add(if (goalReached) RunBadge.GOAL_REACHED else RunBadge.INCOMPLETE)
            if (isDemo) add(RunBadge.DEMO)
        }

    companion object {
        /** Máximo de fotos que admite un recorrido. */
        const val MAX_PHOTOS = 2
    }
}
