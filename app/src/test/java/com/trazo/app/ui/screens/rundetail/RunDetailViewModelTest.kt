package com.trazo.app.ui.screens.rundetail

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.trazo.app.data.repository.SavedRunRepository
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.saved.SavedRun
import com.trazo.app.ui.components.common.StatusBadgeKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Instant

/** Repositorio falso que deja quitar recorridos: el real todavía no borra nada. */
private class MutableSavedRunRepository : SavedRunRepository {
    val current = MutableStateFlow<List<SavedRun>>(emptyList())
    override val runs: Flow<List<SavedRun>> = current

    override suspend fun save(run: SavedRun) {
        current.value = listOf(run) + current.value
    }

    override suspend fun getById(id: String): SavedRun? = current.value.firstOrNull { it.id == id }
}

/** Detalle del recorrido guardado (requisitos 3.1, 3.2, 3.4 a 3.7 y 6.6). Solo lectura. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RunDetailViewModelTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository = MutableSavedRunRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun point(
        latitude: Double,
        longitude: Double,
    ) = RoutePoint(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = 5f,
        speedMetersPerSecond = 2.5f,
        monotonicTimestampMillis = 0L,
    )

    private fun run(
        id: String = "a",
        title: String = "Vuelta al lago",
        goal: RunGoal = RunGoal.TEN_KILOMETERS,
        goalReached: Boolean = true,
        isDemo: Boolean = false,
        highlights: List<String> = emptyList(),
        photoPaths: List<String> = emptyList(),
        routePoints: List<RoutePoint> = listOf(point(-34.6, -58.4), point(-34.61, -58.41)),
    ) = SavedRun(
        id = id,
        title = title,
        startedAt = Instant.fromEpochSeconds(1_700_000_000L),
        goal = goal,
        goalReached = goalReached,
        isDemo = isDemo,
        distanceMeters = 10_250.0,
        elapsedTimeSeconds = 3_300L,
        averageSpeedKmh = 9.5,
        routePoints = routePoints,
        brakes = emptyList(),
        regularityPercent = 80,
        highlights = highlights,
        photoPaths = photoPaths,
    )

    private fun TestScope.subscribed(): RunDetailViewModel {
        val viewModel = RunDetailViewModel(savedRunRepository = repository, context = context)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.detail.collect {} }
        return viewModel
    }

    @Test
    fun `al principio no hay detalle abierto`() =
        runTest {
            repository.save(run())

            assertNull(subscribed().detail.value)
        }

    @Test
    fun `abrir con un id que no existe no abre nada`() =
        runTest {
            repository.save(run(id = "a"))
            val viewModel = subscribed()

            viewModel.open("no-existe")

            assertNull(viewModel.detail.value)
        }

    @Test
    fun `abrir muestra los datos del recorrido`() =
        runTest {
            repository.save(run(id = "a", title = "Vuelta al lago", goal = RunGoal.TEN_KILOMETERS))
            val viewModel = subscribed()

            viewModel.open("a")

            val detail = viewModel.detail.value
            assertNotNull(detail)
            assertEquals("a", detail!!.id)
            assertEquals("Vuelta al lago", detail.title)
            assertEquals("9,5 km/h", detail.averageSpeedText)
            assertEquals("Meta 10 km", detail.goalText)
            assertEquals(2, detail.routePoints.size)
        }

    @Test
    fun `cerrar lo deja en nada`() =
        runTest {
            repository.save(run())
            val viewModel = subscribed()
            viewModel.open("a")
            assertNotNull(viewModel.detail.value)

            viewModel.close()

            assertNull(viewModel.detail.value)
        }

    @Test
    fun `si el recorrido abierto desaparece el detalle se cierra`() =
        runTest {
            repository.save(run())
            val viewModel = subscribed()
            viewModel.open("a")
            assertNotNull(viewModel.detail.value)

            repository.current.value = emptyList()

            assertNull(viewModel.detail.value)
        }

    @Test
    fun `un recorrido que llego a la meta muestra Meta cumplida`() =
        runTest {
            repository.save(run(goalReached = true))
            val viewModel = subscribed()

            viewModel.open("a")

            assertEquals(StatusBadgeKind.GOAL_REACHED, viewModel.detail.value!!.statusBadge)
        }

    @Test
    fun `un recorrido que no llego a la meta muestra Incompleto`() =
        runTest {
            repository.save(run(goalReached = false))
            val viewModel = subscribed()

            viewModel.open("a")

            assertEquals(StatusBadgeKind.INCOMPLETE, viewModel.detail.value!!.statusBadge)
        }

    @Test
    fun `la marca Demo solo se pide para un recorrido demo`() =
        runTest {
            repository.save(run(id = "real", isDemo = false))
            repository.save(run(id = "demo", isDemo = true))
            val viewModel = subscribed()

            viewModel.open("real")
            assertEquals(false, viewModel.detail.value!!.isDemo)
            viewModel.open("demo")
            assertEquals(true, viewModel.detail.value!!.isDemo)
        }

    @Test
    fun `las fotos pasan tal cual sin recortar`() =
        runTest {
            repository.save(run(photoPaths = listOf("/f/1.jpg", "/f/2.jpg", "/f/3.jpg")))
            val viewModel = subscribed()

            viewModel.open("a")

            assertEquals(listOf("/f/1.jpg", "/f/2.jpg", "/f/3.jpg"), viewModel.detail.value!!.photoPaths)
        }

    @Test
    fun `las listas vacias de destacados y fotos quedan vacias`() =
        runTest {
            repository.save(run(highlights = emptyList(), photoPaths = emptyList()))
            val viewModel = subscribed()

            viewModel.open("a")

            assertTrue(
                viewModel.detail.value!!
                    .highlights
                    .isEmpty(),
            )
            assertTrue(
                viewModel.detail.value!!
                    .photoPaths
                    .isEmpty(),
            )
        }

    @Test
    fun `los destacados pasan tal cual`() =
        runTest {
            repository.save(run(highlights = listOf("Ritmo parejo", "Sin frenadas largas")))
            val viewModel = subscribed()

            viewModel.open("a")

            assertEquals(listOf("Ritmo parejo", "Sin frenadas largas"), viewModel.detail.value!!.highlights)
        }

    @Test
    fun `la descripcion del mapa resume titulo distancia y duracion`() =
        runTest {
            repository.save(run(title = "Vuelta al lago"))
            val viewModel = subscribed()

            viewModel.open("a")

            val description = viewModel.detail.value!!.mapDescription
            listOf("Vuelta al lago", "10,3 km", "00:55:00").forEach {
                assertTrue("Falta «$it» en: $description", description.contains(it))
            }
        }
}
