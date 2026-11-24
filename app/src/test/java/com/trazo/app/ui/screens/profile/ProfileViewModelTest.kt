package com.trazo.app.ui.screens.profile

import com.trazo.app.data.repository.FakeSavedRunRepository
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.saved.SavedRun
import com.trazo.app.domain.model.stops.BrakeEvent
import com.trazo.app.ui.components.common.RunValueFormatter
import com.trazo.app.ui.components.common.StatusBadgeKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Instant

/**
 * Perfil lee los recorridos guardados (solo lectura). Que los recorridos sobrevivan al cierre de la
 * app depende de `save-run` (requisito 1.8): el ViewModel muestra lo que el repositorio emita.
 *
 * Los casos del botón «+» pasaron a `StartRunViewModelTest` (start-run, requisito 9).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val repository = FakeSavedRunRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun run(
        id: String,
        startedAtSeconds: Long = 1_700_000_000L,
        title: String = "Recorrido $id",
        distanceMeters: Double = 5_200.0,
        elapsedTimeSeconds: Long = 3_300L,
        goalReached: Boolean = true,
        isDemo: Boolean = false,
        brakeCount: Int = 2,
        regularityPercent: Int? = 80,
    ) = SavedRun(
        id = id,
        title = title,
        startedAt = Instant.fromEpochSeconds(startedAtSeconds),
        goal = RunGoal.FIVE_KILOMETERS,
        goalReached = goalReached,
        isDemo = isDemo,
        distanceMeters = distanceMeters,
        elapsedTimeSeconds = elapsedTimeSeconds,
        averageSpeedKmh = 9.5,
        routePoints = emptyList(),
        brakes = List(brakeCount) { BrakeEvent(id = "$id-$it", runId = id, location = null, detectedAtMillis = it.toLong()) },
        regularityPercent = regularityPercent,
        highlights = emptyList(),
        photoPaths = emptyList(),
    )

    private fun newViewModel() = ProfileViewModel(savedRunRepository = repository)

    /** Mantiene suscripto el estado (usa `WhileSubscribed`) y devuelve el ViewModel. */
    private fun kotlinx.coroutines.test.TestScope.subscribed(): ProfileViewModel {
        val viewModel = newViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun `sin recorridos guardados el estado esta vacio y los totales en cero`() =
        runTest {
            val state = subscribed().uiState.value

            assertEquals(ProfileUiState(), state)
            assertFalse(state.hasRecentRuns)
            assertTrue(state.runs.isEmpty())
            assertEquals(0, state.totals.runCount)
            assertEquals(0.0, state.totals.distanceKm, 0.0)
            assertEquals(0L, state.totals.timeSeconds)
        }

    @Test
    fun `las cards van del recorrido mas reciente al mas antiguo`() =
        runTest {
            repository.save(run("viejo", startedAtSeconds = 100))
            repository.save(run("medio", startedAtSeconds = 200))
            repository.save(run("nuevo", startedAtSeconds = 300))

            val state = subscribed().uiState.value

            assertEquals(listOf("nuevo", "medio", "viejo"), state.runs.map { it.id })
            assertTrue(state.hasRecentRuns)
        }

    @Test
    fun `un recorrido nuevo queda primero y actualiza los totales`() =
        runTest {
            repository.save(run("viejo", startedAtSeconds = 100, distanceMeters = 3_000.0, elapsedTimeSeconds = 1_200L))
            val viewModel = subscribed()
            assertEquals(1, viewModel.uiState.value.totals.runCount)

            repository.save(run("nuevo", startedAtSeconds = 200, distanceMeters = 5_000.0, elapsedTimeSeconds = 1_800L))

            val state = viewModel.uiState.value
            assertEquals(listOf("nuevo", "viejo"), state.runs.map { it.id })
            assertEquals(2, state.totals.runCount)
            assertEquals(8.0, state.totals.distanceKm, 0.0001)
            assertEquals(3_000L, state.totals.timeSeconds)
        }

    @Test
    fun `el primer recorrido reemplaza el estado vacio`() =
        runTest {
            val viewModel = subscribed()
            assertFalse(viewModel.uiState.value.hasRecentRuns)

            repository.save(run("primero"))

            assertTrue(viewModel.uiState.value.hasRecentRuns)
            assertEquals(
                listOf("primero"),
                viewModel.uiState.value.runs
                    .map { it.id },
            )
        }

    @Test
    fun `la card trae titulo fecha distancia duracion frenadas y regularidad formateados`() =
        runTest {
            val saved = run("a", title = "Vuelta al lago", distanceMeters = 5_200.0, elapsedTimeSeconds = 3_300L, brakeCount = 3)
            repository.save(saved)

            val card =
                subscribed()
                    .uiState.value.runs
                    .single()

            assertEquals("a", card.id)
            assertEquals("Vuelta al lago", card.title)
            assertEquals(RunValueFormatter.formatShortDate(saved.startedAt), card.dateText)
            assertEquals("5,2 km", card.distanceText)
            assertEquals("00:55:00", card.durationText)
            assertEquals(3, card.brakeCount)
            assertEquals(80, card.regularityPercent)
        }

    @Test
    fun `un recorrido completo y real no lleva marcas`() =
        runTest {
            repository.save(run("a", goalReached = true, isDemo = false))

            assertEquals(
                emptyList<StatusBadgeKind>(),
                subscribed()
                    .uiState.value.runs
                    .single()
                    .badges,
            )
        }

    @Test
    fun `un recorrido que no llego a la meta lleva la marca Incompleto`() =
        runTest {
            repository.save(run("a", goalReached = false))

            assertEquals(
                listOf(StatusBadgeKind.INCOMPLETE),
                subscribed()
                    .uiState.value.runs
                    .single()
                    .badges,
            )
        }

    @Test
    fun `un recorrido demo lleva la marca Demo`() =
        runTest {
            repository.save(run("a", isDemo = true))

            assertEquals(
                listOf(StatusBadgeKind.DEMO),
                subscribed()
                    .uiState.value.runs
                    .single()
                    .badges,
            )
        }

    @Test
    fun `un recorrido incompleto y demo lleva las dos marcas juntas`() =
        runTest {
            repository.save(run("a", goalReached = false, isDemo = true))

            assertEquals(
                listOf(StatusBadgeKind.INCOMPLETE, StatusBadgeKind.DEMO),
                subscribed()
                    .uiState.value.runs
                    .single()
                    .badges,
            )
        }

    @Test
    fun `los recorridos demo se listan pero no suman a los totales`() =
        runTest {
            repository.save(run("demo", isDemo = true, distanceMeters = 9_000.0, elapsedTimeSeconds = 5_000L))

            val state = subscribed().uiState.value

            assertTrue(state.hasRecentRuns)
            assertEquals(1, state.runs.size)
            assertEquals(0, state.totals.runCount)
            assertEquals(0.0, state.totals.distanceKm, 0.0)
            assertEquals(0L, state.totals.timeSeconds)
        }

    @Test
    fun `los incompletos suman a los totales`() =
        runTest {
            repository.save(run("a", goalReached = false, distanceMeters = 2_000.0, elapsedTimeSeconds = 900L))

            val state = subscribed().uiState.value

            assertEquals(1, state.totals.runCount)
            assertEquals(2.0, state.totals.distanceKm, 0.0001)
            assertEquals(900L, state.totals.timeSeconds)
        }

    @Test
    fun `sin recorridos las barras de estadisticas quedan vacias`() =
        runTest {
            assertEquals(ProfileStatsFill(0f, 0f, 0f), subscribed().uiState.value.statsFill)
        }

    @Test
    fun `las barras se llenan en proporcion al tope de cada metrica`() =
        runTest {
            // Topes: 20 recorridos, 50 km y 10 h.
            repository.save(run("a", startedAtSeconds = 100, distanceMeters = 10_000.0, elapsedTimeSeconds = 3_600L))
            repository.save(run("b", startedAtSeconds = 200, distanceMeters = 15_000.0, elapsedTimeSeconds = 5_400L))

            val fill = subscribed().uiState.value.statsFill

            assertEquals(0.1f, fill.runs, 0.0001f)
            assertEquals(0.5f, fill.distance, 0.0001f)
            assertEquals(0.25f, fill.time, 0.0001f)
        }

    @Test
    fun `al superar el tope la barra queda llena`() =
        runTest {
            repository.save(run("largo", distanceMeters = 80_000.0, elapsedTimeSeconds = 50_000L))

            val fill = subscribed().uiState.value.statsFill

            assertEquals(1f, fill.distance, 0f)
            assertEquals(1f, fill.time, 0f)
        }

    @Test
    fun `los recorridos demo no llenan las barras`() =
        runTest {
            repository.save(run("demo", isDemo = true, distanceMeters = 9_000.0, elapsedTimeSeconds = 5_000L))

            assertEquals(ProfileStatsFill(), subscribed().uiState.value.statsFill)
        }

    @Test
    fun `sin dato de regularidad la card trae el porcentaje en null`() =
        runTest {
            repository.save(run("a", regularityPercent = null))

            val card =
                subscribed()
                    .uiState.value.runs
                    .single()

            assertNull(card.regularityPercent)
        }
}
