package com.trazo.app.domain.model.saved

import com.trazo.app.domain.model.map.RoutePoint
import kotlin.math.cos
import kotlin.math.hypot

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val MIN_POINTS_FOR_TRACE = 2

/**
 * Verdadero si algún tramo de la traza queda a [toleranceMeters] o menos del punto. Una traza de menos
 * de 2 puntos nunca coincide. Usa un plano local equirectangular centrado en el punto tocado, suficiente
 * para recorridos urbanos.
 */
fun SavedRun.isTraceNear(
    latitude: Double,
    longitude: Double,
    toleranceMeters: Double,
): Boolean {
    if (routePoints.size < MIN_POINTS_FOR_TRACE) return false
    val metersPerLongitudeDegree = Math.toRadians(1.0) * EARTH_RADIUS_METERS * cos(Math.toRadians(latitude))
    val metersPerLatitudeDegree = Math.toRadians(1.0) * EARTH_RADIUS_METERS

    fun RoutePoint.x() = (this.longitude - longitude) * metersPerLongitudeDegree

    fun RoutePoint.y() = (this.latitude - latitude) * metersPerLatitudeDegree

    return routePoints.zipWithNext().any { (from, to) ->
        distanceToSegment(from.x(), from.y(), to.x(), to.y()) <= toleranceMeters
    }
}

/** El de mayor `startedAt` entre los que cumplen [isTraceNear]; null si ninguno. Con empate gana el primero. */
fun List<SavedRun>.mostRecentNear(
    latitude: Double,
    longitude: Double,
    toleranceMeters: Double,
): SavedRun? =
    filter { it.isTraceNear(latitude, longitude, toleranceMeters) }
        .fold(null) { best: SavedRun?, candidate ->
            if (best == null || candidate.startedAt > best.startedAt) candidate else best
        }

/** Distancia del origen al segmento (ax, ay)-(bx, by), en el mismo plano. */
private fun distanceToSegment(
    ax: Double,
    ay: Double,
    bx: Double,
    by: Double,
): Double {
    val dx = bx - ax
    val dy = by - ay
    val lengthSquared = dx * dx + dy * dy
    if (lengthSquared == 0.0) return hypot(ax, ay)
    val t = (-(ax * dx + ay * dy) / lengthSquared).coerceIn(0.0, 1.0)
    return hypot(ax + t * dx, ay + t * dy)
}
