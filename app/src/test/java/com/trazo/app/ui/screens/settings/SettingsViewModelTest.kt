package com.trazo.app.ui.screens.settings

import com.trazo.app.data.repository.FakeDemoModeRepository
import com.trazo.app.data.repository.FakePermissionRepository
import com.trazo.app.data.repository.FakeRunSessionRepository
import com.trazo.app.domain.model.CameraPermissionStatus
import com.trazo.app.domain.model.LocationPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePermissionRepository: FakePermissionRepository
    private lateinit var fakeDemoModeRepository: FakeDemoModeRepository
    private lateinit var fakeRunSessionRepository: FakeRunSessionRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakePermissionRepository = FakePermissionRepository()
        fakeDemoModeRepository = FakeDemoModeRepository()
        fakeRunSessionRepository = FakeRunSessionRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initialValueBeforeCollecting_reflectsRepositoriesInitialState() {
        fakePermissionRepository.locationState = LocationPermissionState.APPROXIMATE
        fakePermissionRepository.cameraStatus = CameraPermissionStatus.DENIED
        fakeRunSessionRepository.setActive(id = "active-run-0")

        viewModel =
            SettingsViewModel(
                permissionRepository = fakePermissionRepository,
                demoModeRepository = fakeDemoModeRepository,
                runSessionRepository = fakeRunSessionRepository,
            )

        val initialState = viewModel.uiState.value
        assertEquals(LocationPermissionState.APPROXIMATE, initialState.locationPermissionState)
        assertEquals(CameraPermissionStatus.DENIED, initialState.cameraPermissionStatus)
        assertTrue(initialState.isRunInProgress)
        assertFalse(initialState.isDemoModeSwitchEnabled)
        assertTrue(initialState.showLocationSettingsButton)
        assertTrue(initialState.showCameraSettingsButton)
    }

    @Test
    fun uiState_reflectsInitialPermissionStatesAndButtonVisibility() =
        runTest {
            viewModel =
                SettingsViewModel(
                    permissionRepository = fakePermissionRepository,
                    demoModeRepository = fakeDemoModeRepository,
                    runSessionRepository = fakeRunSessionRepository,
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(LocationPermissionState.EXACT, state.locationPermissionState)
            assertEquals(CameraPermissionStatus.GRANTED, state.cameraPermissionStatus)
            assertFalse(state.showLocationSettingsButton)
            assertFalse(state.showCameraSettingsButton)
            assertTrue(state.isDemoModeSwitchEnabled)
            assertFalse(state.isRunInProgress)
        }

    @Test
    fun uiState_showsLocationSettingsButton_whenLocationIsNotExact() =
        runTest {
            fakePermissionRepository.locationState = LocationPermissionState.APPROXIMATE

            viewModel =
                SettingsViewModel(
                    permissionRepository = fakePermissionRepository,
                    demoModeRepository = fakeDemoModeRepository,
                    runSessionRepository = fakeRunSessionRepository,
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(LocationPermissionState.APPROXIMATE, state.locationPermissionState)
            assertTrue(state.showLocationSettingsButton)
        }

    @Test
    fun uiState_showsCameraSettingsButton_whenCameraIsDeniedOrUnknown() =
        runTest {
            fakePermissionRepository.cameraStatus = CameraPermissionStatus.DENIED

            viewModel =
                SettingsViewModel(
                    permissionRepository = fakePermissionRepository,
                    demoModeRepository = fakeDemoModeRepository,
                    runSessionRepository = fakeRunSessionRepository,
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            testScheduler.advanceUntilIdle()

            val stateDenied = viewModel.uiState.value
            assertEquals(CameraPermissionStatus.DENIED, stateDenied.cameraPermissionStatus)
            assertTrue(stateDenied.showCameraSettingsButton)

            fakePermissionRepository.cameraStatus = CameraPermissionStatus.UNKNOWN
            viewModel.onRefreshPermissions()
            testScheduler.advanceUntilIdle()

            val stateUnknown = viewModel.uiState.value
            assertEquals(CameraPermissionStatus.UNKNOWN, stateUnknown.cameraPermissionStatus)
            assertTrue(stateUnknown.showCameraSettingsButton)
        }

    @Test
    fun onRefreshPermissions_reevaluatesPermissionRepository() =
        runTest {
            viewModel =
                SettingsViewModel(
                    permissionRepository = fakePermissionRepository,
                    demoModeRepository = fakeDemoModeRepository,
                    runSessionRepository = fakeRunSessionRepository,
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            testScheduler.advanceUntilIdle()

            assertEquals(LocationPermissionState.EXACT, viewModel.uiState.value.locationPermissionState)

            fakePermissionRepository.locationState = LocationPermissionState.DENIED
            viewModel.onRefreshPermissions()
            testScheduler.advanceUntilIdle()

            assertEquals(LocationPermissionState.DENIED, viewModel.uiState.value.locationPermissionState)
            assertTrue(viewModel.uiState.value.showLocationSettingsButton)
        }

    @Test
    fun onDemoModeToggled_updatesDemoMode_whenNoRunIsInProgress() =
        runTest {
            viewModel =
                SettingsViewModel(
                    permissionRepository = fakePermissionRepository,
                    demoModeRepository = fakeDemoModeRepository,
                    runSessionRepository = fakeRunSessionRepository,
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            testScheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isDemoModeEnabled)

            viewModel.onDemoModeToggled(true)
            testScheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isDemoModeEnabled)
            assertEquals(listOf(true), fakeDemoModeRepository.setDemoModeCalls)
        }

    @Test
    fun onDemoModeToggled_ignoresToggleAndDoesNotPersist_whenRunIsInProgress() =
        runTest {
            viewModel =
                SettingsViewModel(
                    permissionRepository = fakePermissionRepository,
                    demoModeRepository = fakeDemoModeRepository,
                    runSessionRepository = fakeRunSessionRepository,
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }

            fakeRunSessionRepository.setActive(id = "active-run-1")
            testScheduler.advanceUntilIdle()

            val stateBefore = viewModel.uiState.value
            assertTrue(stateBefore.isRunInProgress)
            assertFalse(stateBefore.isDemoModeSwitchEnabled)

            viewModel.onDemoModeToggled(true)
            testScheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isDemoModeEnabled)
            assertTrue(fakeDemoModeRepository.setDemoModeCalls.isEmpty())
        }
}
