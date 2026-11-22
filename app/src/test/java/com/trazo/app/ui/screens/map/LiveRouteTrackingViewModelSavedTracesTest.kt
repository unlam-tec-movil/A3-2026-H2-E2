package com.trazo.app.ui.screens.map

import com.trazo.app.data.repository.FakeLocationRepository
import com.trazo.app.data.repository.FakeRunSessionRepository
import com.trazo.app.data.repository.FakeSavedRunRepository
import com.trazo.app.domain.model.LocationStatus
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.saved.SavedRun
import com.trazo.app.domain.usecases.testFinishRunUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Instant

/** Trazas guardadas que el mapa de Actividad dibuja (profile-runs, requisitos 5.1 y 5.3 a 5.6). */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveRouteTrackingViewModelSavedTracesTest {
    private val sessionRepository = FakeRunSessionRepository()
    private val savedRunRepository = FakeSavedRunRepository()
    private var locationStatus = LocationStatus.READY
    private val locationRepository = FakeLocationRepository(locationStatus = { locationStatus })

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.subscribedViewModel(): LiveRouteTrackingViewModel {
        val viewModel =
            LiveRouteTrackingViewModel(
                runSessionRepository = sessionRepository,
                locationRepository = locationRepository,
                finishRun = testFinishRunUseCase(sessionRepository),
                savedRunRepository = savedRunRepository,
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun LiveRouteTrackingViewModel.traces() = uiState.value.savedTraces

    private fun LiveRouteTrackingViewModel.traceIds() = traces().map { it.id }

    private fun point(index: Int) =
        RoutePoint(
            latitude = -34.6 + index * 0.001,
            longitude = -58.4,
            accuracyMeters = 5f,
            speedMetersPerSecond = 2f,
            monotonicTimestampMillis = index.toLong(),
        )

    private fun run(
        id: String,
        startedAtSeconds: Long = 1_700_000_000L,
    ) = SavedRun(
        id = id,
        title = "Recorrido $id",
        startedAt = Instant.fromEpochSeconds(startedAtSeconds),
        goal = RunGoal.FIVE_KILOMETERS,
        goalReached = true,
        isDemo = false,
        distanceMeters = 5_200.0,
        elapsedTimeSeconds = 3_300L,
        averageSpeedKmh = 9.5,
        routePoints = List(3) { point(it) },
        brakes = emptyList(),
        regularityPercent = 80,
        highlights = emptyList(),
        photoPaths = emptyList(),
    )

    @Test
    fun `sin recorridos guardados no hay trazas`() =
        runTest {
            val viewModel = subscribedViewModel()

            assertTrue(viewModel.traces().isEmpty())
        }

    @Test
    fun `con recorridos, sin recorrido en curso y ubicacion precisa hay una traza por recorrido`() =
        runTest {
            savedRunRepository.save(run("a", startedAtSeconds = 1_700_000_000L))
            savedRunRepository.save(run("b", startedAtSeconds = 1_700_100_000L))

            val traces = subscribedViewModel().traces()

            assertEquals(listOf("b", "a"), traces.map { it.id })
            assertEquals(List(3) { point(it) }, traces.first().points)
        }

    @Test
    fun `con un recorrido en curso no hay trazas`() =
        runTest {
            savedRunRepository.save(run("a"))
            sessionRepository.setActive(id = "en-curso")

            assertTrue(subscribedViewModel().traces().isEmpty())
        }

    @Test
    fun `al iniciar un recorrido las trazas desaparecen y vuelven al terminarlo`() =
        runTest {
            savedRunRepository.save(run("a"))
            val viewModel = subscribedViewModel()
            assertEquals(listOf("a"), viewModel.traceIds())

            sessionRepository.setActive(id = "en-curso")
            assertTrue(viewModel.traces().isEmpty())

            sessionRepository.resetToIdle()
            assertEquals(listOf("a"), viewModel.traceIds())
        }

    @Test
    fun `con ubicacion aproximada no hay trazas`() =
        runTest {
            savedRunRepository.save(run("a"))
            locationStatus = LocationStatus.APPROXIMATE_ONLY

            assertTrue(subscribedViewModel().traces().isEmpty())
        }

    @Test
    fun `con ubicacion del dispositivo apagada o sin permiso no hay trazas`() =
        runTest {
            savedRunRepository.save(run("a"))

            locationStatus = LocationStatus.DEVICE_LOCATION_OFF
            assertTrue(subscribedViewModel().traces().isEmpty())

            locationStatus = LocationStatus.PERMISSION_DENIED
            assertTrue(subscribedViewModel().traces().isEmpty())
        }

    @Test
    fun `al releer la ubicacion con permiso preciso aparecen las trazas`() =
        runTest {
            savedRunRepository.save(run("a"))
            locationStatus = LocationStatus.APPROXIMATE_ONLY
            val viewModel = subscribedViewModel()
            assertTrue(viewModel.traces().isEmpty())

            locationStatus = LocationStatus.READY
            viewModel.onLocationStatusRefreshed()

            assertEquals(listOf("a"), viewModel.traceIds())
        }

    @Test
    fun `al perderse el permiso preciso al releer las trazas desaparecen`() =
        runTest {
            savedRunRepository.save(run("a"))
            val viewModel = subscribedViewModel()
            assertEquals(listOf("a"), viewModel.traceIds())

            locationStatus = LocationStatus.PERMISSION_DENIED
            viewModel.onLocationStatusRefreshed()

            assertTrue(viewModel.traces().isEmpty())
        }

    @Test
    fun `al guardarse un recorrido aparece su traza`() =
        runTest {
            savedRunRepository.save(run("a", startedAtSeconds = 1_700_000_000L))
            val viewModel = subscribedViewModel()

            savedRunRepository.save(run("nuevo", startedAtSeconds = 1_700_200_000L))

            assertEquals(listOf("nuevo", "a"), viewModel.traceIds())
        }

    @Test
    fun `las trazas guardadas no se mezclan con la traza en vivo`() =
        runTest {
            savedRunRepository.save(run("a"))
            val viewModel = subscribedViewModel()

            assertTrue(viewModel.uiState.value.let { it.routePoints.isEmpty() })
        }
}
