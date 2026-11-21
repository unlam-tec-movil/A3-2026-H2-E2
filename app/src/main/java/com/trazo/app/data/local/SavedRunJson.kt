package com.trazo.app.data.local

import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.stops.BrakeEvent
import com.trazo.app.domain.model.stops.StopLocation
import org.json.JSONArray
import org.json.JSONObject

/** Convierte la ruta a un texto JSON para guardarla en una sola columna. */
internal fun List<RoutePoint>.routeToJson(): String {
    val array = JSONArray()
    forEach { point ->
        array.put(
            JSONObject()
                .put("lat", point.latitude)
                .put("lng", point.longitude)
                .put("acc", point.accuracyMeters.toDouble())
                .put("speed", point.speedMetersPerSecond.toDouble())
                .put("t", point.monotonicTimestampMillis),
        )
    }
    return array.toString()
}

/** Hace el camino inverso: del texto JSON a la lista de puntos. */
internal fun String.jsonToRoute(): List<RoutePoint> {
    val array = JSONArray(this)
    return List(array.length()) { index ->
        val item = array.getJSONObject(index)
        RoutePoint(
            latitude = item.getDouble("lat"),
            longitude = item.getDouble("lng"),
            accuracyMeters = item.getDouble("acc").toFloat(),
            speedMetersPerSecond = item.getDouble("speed").toFloat(),
            monotonicTimestampMillis = item.getLong("t"),
        )
    }
}

internal fun List<BrakeEvent>.brakesToJson(): String {
    val array = JSONArray()
    forEach { brake ->
        val item =
            JSONObject()
                .put("id", brake.id)
                .put("runId", brake.runId)
                .put("at", brake.detectedAtMillis)
        brake.location?.let { location ->
            item.put(
                "loc",
                JSONObject()
                    .put("lat", location.latitude)
                    .put("lng", location.longitude)
                    .put("acc", location.accuracyMeters.toDouble()),
            )
        }
        array.put(item)
    }
    return array.toString()
}

internal fun String.jsonToBrakes(): List<BrakeEvent> {
    val array = JSONArray(this)
    return List(array.length()) { index ->
        val item = array.getJSONObject(index)
        val location = item.optJSONObject("loc")
        BrakeEvent(
            id = item.getString("id"),
            runId = item.getString("runId"),
            location =
                location?.let {
                    StopLocation(
                        latitude = it.getDouble("lat"),
                        longitude = it.getDouble("lng"),
                        accuracyMeters = it.getDouble("acc").toFloat(),
                    )
                },
            detectedAtMillis = item.getLong("at"),
        )
    }
}

/** Para los destacados y las rutas de las fotos: una lista de textos. */
internal fun List<String>.textsToJson(): String = JSONArray(this).toString()

internal fun String.jsonToTexts(): List<String> {
    val array = JSONArray(this)
    return List(array.length()) { index -> array.getString(index) }
}
