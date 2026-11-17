package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.RawLocationPoint
import com.trazo.app.domain.repository.LocationClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class FakeLocationClient : LocationClient {
    private val locationUpdates =
        MutableSharedFlow<RawLocationPoint>(
            replay = 0,
            extraBufferCapacity = 10,
        )

    private val signalQuality =
        MutableStateFlow(GpsQuality.NO_SIGNAL)

    private var shouldFailWithSecurityException = false

    override fun getLocationUpdates(intervalMillis: Long): Flow<RawLocationPoint> =
        flow {
            if (shouldFailWithSecurityException) {
                throw SecurityException("No se concedieron permisos de ubicación.")
            }
            locationUpdates.collect { emit(it) }
        }

    override fun getSignalQualityUpdates(): Flow<GpsQuality> = signalQuality.asStateFlow()

    suspend fun awaitLocationSubscriber() {
        locationUpdates.subscriptionCount.first { it > 0 }
    }

    fun sendLocation(point: RawLocationPoint) {
        locationUpdates.tryEmit(point)
    }

    fun sendSignalQuality(quality: GpsQuality) {
        signalQuality.value = quality
    }

    fun triggerSecurityException() {
        shouldFailWithSecurityException = true
    }
}
