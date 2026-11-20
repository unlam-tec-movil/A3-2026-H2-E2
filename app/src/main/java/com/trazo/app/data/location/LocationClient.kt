package com.trazo.app.data.location

import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.RoutePoint
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun getLocationUpdates(intervalMillis: Long): Flow<RoutePoint>

    fun getSignalQualityUpdates(): Flow<GpsQuality>
}
