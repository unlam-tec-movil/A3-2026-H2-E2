package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.repository.LocationClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AwaitPreciseLocationUseCaseTest {
    private fun point(accuracy: Float) =
        RoutePoint(
            latitude = -34.6,
            longitude = -58.4,
            accuracyMeters = accuracy,
            speedMetersPerSecond = 0f,
            monotonicTimestampMillis = 0L,
        )

    private fun clientOf(updates: Flow<RoutePoint>) =
        object : LocationClient {
            override fun getLocationUpdates(intervalMillis: Long): Flow<RoutePoint> = updates

            override fun getSignalQualityUpdates(): Flow<GpsQuality> = emptyFlow()
        }

    // 8.2, 8.3, 10.1
    @Test
    fun ignoresImpreciseLocations_andReturnsFirstWithin30Meters() =
        runTest {
            val useCase = AwaitPreciseLocationUseCase(clientOf(flowOf(point(31f), point(30f), point(5f))), GpsPointFilterPolicy())

            assertEquals(30f, useCase().accuracyMeters)
        }

    // 8.5
    @Test
    fun propagatesTheErrorOfTheLocationFlow() =
        runTest {
            val useCase =
                AwaitPreciseLocationUseCase(
                    clientOf(flow { throw SecurityException("sin permiso") }),
                    GpsPointFilterPolicy(),
                )

            val result = runCatching { useCase() }

            assertTrue(result.exceptionOrNull() is SecurityException)
        }
}
