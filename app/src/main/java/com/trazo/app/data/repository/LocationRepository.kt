package com.trazo.app.data.repository

import com.trazo.app.domain.model.LocationStatus
import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.RoutePoint
import kotlinx.coroutines.flow.Flow

/**
 * Ubicación del dispositivo para la UI y los casos de uso: puntos, calidad de señal y estado del
 * permiso. Oculta las fuentes (cliente de GPS y lector de estado de Android).
 */
interface LocationRepository {
    /** Puntos de ubicación cada [intervalMillis]; falla si no hay permiso. */
    fun getLocationUpdates(intervalMillis: Long): Flow<RoutePoint>

    fun getSignalQualityUpdates(): Flow<GpsQuality>

    /** Estado del permiso y de la ubicación del dispositivo en este momento. */
    fun readLocationStatus(): LocationStatus
}
