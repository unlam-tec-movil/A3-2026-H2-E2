package com.trazo.app.domain.model.map

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

/** Distancia en metros sobre la superficie terrestre hasta [other] (fórmula de Haversine). */
fun RoutePoint.distanceMetersTo(other: RoutePoint): Double {
    val latitudeDifference = Math.toRadians(other.latitude - latitude)
    val longitudeDifference = Math.toRadians(other.longitude - longitude)
    val firstLatitude = Math.toRadians(latitude)
    val secondLatitude = Math.toRadians(other.latitude)

    val a =
        sin(latitudeDifference / 2).let { it * it } +
            cos(firstLatitude) * cos(secondLatitude) * sin(longitudeDifference / 2).let { it * it }

    val centralAngle = 2 * atan2(sqrt(a.coerceIn(0.0, 1.0)), sqrt((1.0 - a).coerceAtLeast(0.0)))

    return EARTH_RADIUS_METERS * centralAngle
}
