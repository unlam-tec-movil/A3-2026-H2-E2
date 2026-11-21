package com.trazo.app.data.local

import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.saved.SavedRun
import com.trazo.app.domain.model.stops.BrakeEvent
import com.trazo.app.domain.model.stops.StopLocation
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class SavedRunMapperTest {
    @Test
    fun `un recorrido convertido a entidad y de vuelta es igual al original`() {
        val original =
            SavedRun(
                id = "r1",
                title = "Mi carrera",
                startedAt = Instant.fromEpochMilliseconds(1_000),
                goal = RunGoal.FIVE_KILOMETERS,
                goalReached = true,
                isDemo = false,
                distanceMeters = 5010.5,
                elapsedTimeSeconds = 1800,
                averageSpeedKmh = 10.0,
                routePoints =
                    listOf(
                        RoutePoint(-34.6, -58.4, 5f, 2.5f, 100L),
                        RoutePoint(-34.7, -58.5, 4f, 3f, 200L),
                    ),
                brakes =
                    listOf(
                        BrakeEvent("b1", "r1", StopLocation(-34.6, -58.4, 5f), 500L),
                        BrakeEvent("b2", "r1", null, 900L),
                    ),
                regularityPercent = null,
                highlights = listOf("Sin parar", "Con viento"),
                photoPaths = listOf("/fotos/1.jpg"),
            )

        val result = original.toEntity().toDomain()

        assertEquals(original, result)
    }
}