package com.trazo.app.data.repository

import com.trazo.app.data.location.FakeLocationClient
import com.trazo.app.domain.model.LocationStatus
import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.RoutePoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationRepositoryImplTest {
    private val client = FakeLocationClient()
    private var status = LocationStatus.READY
    private val repository = LocationRepositoryImpl(client) { status }

    @Test
    fun `los puntos de ubicacion vienen del cliente de GPS`() =
        runTest {
            val point = RoutePoint(-34.6, -58.4, 5f, 2f, 1_000L)
            var received: RoutePoint? = null
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                received = repository.getLocationUpdates(intervalMillis = 1_000L).first()
            }
            client.awaitLocationSubscriber()

            client.sendLocation(point)

            assertEquals(point, received)
        }

    @Test
    fun `la calidad de senal viene del cliente de GPS`() =
        runTest {
            client.sendSignalQuality(GpsQuality.FULL_SIGNAL)

            assertEquals(GpsQuality.FULL_SIGNAL, repository.getSignalQualityUpdates().first())
        }

    @Test
    fun `el estado de la ubicacion viene del lector de Android`() {
        assertEquals(LocationStatus.READY, repository.readLocationStatus())

        status = LocationStatus.DEVICE_LOCATION_OFF

        assertEquals(LocationStatus.DEVICE_LOCATION_OFF, repository.readLocationStatus())
    }
}
