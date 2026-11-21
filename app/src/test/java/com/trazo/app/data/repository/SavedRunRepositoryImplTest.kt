package com.trazo.app.data.repository

import androidx.room.Room
import com.trazo.app.data.local.AppDatabase
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.saved.SavedRun
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class SavedRunRepositoryImplTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: SavedRunRepositoryImpl

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = SavedRunRepositoryImpl(database.savedRunDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `los recorridos se listan del mas reciente al mas antiguo`() =
        runTest {
            repository.save(run("viejo", 100))
            repository.save(run("nuevo", 200))

            val ids = repository.runs.first().map { it.id }

            assertEquals(listOf("nuevo", "viejo"), ids)
        }

    @Test
    fun `getById devuelve el recorrido guardado`() =
        runTest {
            repository.save(run("r1", 100))

            assertEquals("r1", repository.getById("r1")?.id)
        }

    @Test
    fun `getById devuelve null si el recorrido no existe`() =
        runTest {
            assertNull(repository.getById("no-existe"))
        }

    private fun run(
        id: String,
        startedAtSeconds: Long,
    ) = SavedRun(
        id = id,
        title = "Recorrido $id",
        startedAt = Instant.fromEpochSeconds(startedAtSeconds),
        goal = RunGoal.THREE_KILOMETERS,
        goalReached = true,
        isDemo = false,
        distanceMeters = 3000.0,
        elapsedTimeSeconds = 1200,
        averageSpeedKmh = 9.0,
        routePoints = emptyList(),
        brakes = emptyList(),
        regularityPercent = null,
        highlights = emptyList(),
        photoPaths = emptyList(),
    )
}
