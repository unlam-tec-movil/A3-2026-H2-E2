package com.trazo.app.data.repository

import com.trazo.app.domain.model.LocationStatus
import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.RoutePoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Fake de [LocationRepository] para tests JVM. Cada respuesta es una función que se evalúa en cada
 * llamada, así el test puede cambiar lo que devuelve sin recrear el fake.
 */
class FakeLocationRepository(
    var locationUpdates: () -> Flow<RoutePoint> = { emptyFlow() },
    var signalQualityUpdates: () -> Flow<GpsQuality> = { emptyFlow() },
    var locationStatus: () -> LocationStatus = { LocationStatus.READY },
) : LocationRepository {
    override fun getLocationUpdates(intervalMillis: Long): Flow<RoutePoint> = locationUpdates()

    override fun getSignalQualityUpdates(): Flow<GpsQuality> = signalQualityUpdates()

    override fun readLocationStatus(): LocationStatus = locationStatus()
}
