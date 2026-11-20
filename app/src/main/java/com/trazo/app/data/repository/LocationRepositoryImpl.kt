package com.trazo.app.data.repository

import com.trazo.app.data.location.LocationClient
import com.trazo.app.data.location.LocationStatusReader
import com.trazo.app.domain.model.LocationStatus
import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.RoutePoint
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Delega en el cliente de GPS y en el lector de estado de Android, sin reglas propias. */
class LocationRepositoryImpl
    @Inject
    constructor(
        private val locationClient: LocationClient,
        private val locationStatusReader: LocationStatusReader,
    ) : LocationRepository {
        override fun getLocationUpdates(intervalMillis: Long): Flow<RoutePoint> = locationClient.getLocationUpdates(intervalMillis)

        override fun getSignalQualityUpdates(): Flow<GpsQuality> = locationClient.getSignalQualityUpdates()

        override fun readLocationStatus(): LocationStatus = locationStatusReader.read()
    }
