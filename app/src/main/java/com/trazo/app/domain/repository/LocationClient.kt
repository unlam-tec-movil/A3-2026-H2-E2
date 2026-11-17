package com.trazo.app.domain.repository

import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.RawLocationPoint
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun getLocationUpdates(intervalMillis: Long): Flow<RawLocationPoint>

    fun getSignalQualityUpdates(): Flow<GpsQuality>
}
