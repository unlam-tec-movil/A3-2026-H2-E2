package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.RawLocationPoint
import com.trazo.app.domain.repository.LocationClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class FakeLocationClient : LocationClient {
    private val locationUpdates =
        MutableSharedFlow<RawLocationPoint>(
            replay = 0,
            extraBufferCapacity = 10,
        )

    private val signalQuality =
        MutableStateFlow(GpsQuality.NO_SIGNAL)

    override fun getLocationUpdates(intervalMillis: Long) = locationUpdates.asSharedFlow()

    override fun getSignalQualityUpdates() = signalQuality.asStateFlow()

    suspend fun awaitLocationSubscriber() {
        locationUpdates.subscriptionCount.first { it > 0 }
    }

    fun sendLocation(point: RawLocationPoint) {
        locationUpdates.tryEmit(point)
    }

    fun sendSignalQuality(quality: GpsQuality) {
        signalQuality.value = quality
    }
}
