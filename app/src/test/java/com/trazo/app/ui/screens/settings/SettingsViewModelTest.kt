package com.trazo.app.ui.screens.settings

import com.trazo.app.data.repository.FakeDemoModeRepository
import com.trazo.app.data.repository.FakeLocationRepository
import com.trazo.app.data.repository.FakePermissionRepository
import com.trazo.app.data.repository.FakeRunSessionRepository
import com.trazo.app.domain.model.CameraPermissionStatus
import com.trazo.app.domain.model.LocationStatus
import com.trazo.app.domain.usecases.SetDemoModeUseCase
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
    private lateinit var fakeLocationRepository: FakeLocationRepository
    private lateinit var fakePermissionRepository: FakePermissionRepository
    private lateinit var fakeDemoModeRepository: FakeDemoModeRepository
    private lateinit var fakeRunSessionRepository: FakeRunSessionRepository
    private lateinit var setDemoModeUseCase: SetDemoModeUseCase
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeLocationRepository = FakeLocationRepository()
        fakePermissionRepository = FakePermissionRepository()
        fakeDemoModeRepository = FakeDemoModeRepository()
        fakeRunSessionRepository = FakeRunSessionRepository()
        setDemoModeUseCase = SetDemoModeUseCase(fakeDemoModeRepository, fakeRunSessionRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(
            locationRepository = fakeLocationRepository,
            permissionRepository = fakePermissionRepository,
            demoModeRepository = fakeDemoModeRepository,
            runSessionRepository = fakeRunSessionRepository,
            setDemoModeUseCase = setDemoModeUseCase,
        )

    @Test
    fun uiState_initialValueBeforeCollecting_reflectsRepositoriesInitialState() {
        fakeLocationRepository.locationStatus = { LocationStatus.APPROXIMATE_ONLY }
        fakePermissionRepository.cameraStatus = CameraPermissionStatus.DENIED
        fakeRunSessionRepository.setActive(id = "active-run-0")

        viewModel = createViewModel()

        val initialState = viewModel.uiState.value
        assertEquals(LocationStatus.APPROXIMATE_ONLY, initialState.locationStatus)
        assertEquals(CameraPermissionStatus.DENIED, initialState.cameraPermissionStatus)
        assertTrue(initialState.isRunInProgress)
        assertFalse(initialState.isDemoModeSwitchEnabled)
        assertTrue(initialState.showLocationSettingsButton)
        assertTrue(initialState.showCameraSettingsButton)
        assertFalse(initialState.arePermissionsGranted)
    }

    @Test
    fun uiState_reflectsInitialPermissionStatesAndButtonVisibility() =
        runTest {
            viewModel = createViewModel()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(LocationStatus.READY, state.locationStatus)
            assertEquals(CameraPermissionStatus.GRANTED, state.cameraPermissionStatus)
            assertFalse(state.showLocationSettingsButton)
            assertFalse(state.showCameraSettingsButton)
            assertTrue(state.arePermissionsGranted)
            assertTrue(state.isDemoModeSwitchEnabled)
            assertFalse(state.isRunInProgress)
        }

    @Test
    fun uiState_showsLocationSettingsButton_whenLocationIsApproximateOrDenied() =
        runTest {
            fakeLocationRepository.locationStatus = { LocationStatus.APPROXIMATE_ONLY }

            viewModel = createViewModel()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            testScheduler.advanceUntilIdle()

            val stateApproximate = viewModel.uiState.value
            assertEquals(LocationStatus.APPROXIMATE_ONLY, stateApproximate.locationStatus)
            assertTrue(stateApproximate.showLocationSettingsButton)

            fakeLocationRepository.locationStatus = { LocationStatus.PERMISSION_DENIED }
            viewModel.onRefreshPermissions()
            testScheduler.advanceUntilIdle()

            val stateDenied = viewModel.uiState.value
            assertEquals(LocationStatus.PERMISSION_DENIED, stateDenied.locationStatus)
            assertTrue(stateDenied.showLocationSettingsButton)

            fakeLocationRepository.locationStatus = { LocationStatus.DEVICE_LOCATION_OFF }
            viewModel.onRefreshPermissions()
            testScheduler.advanceUntilIdle()

            val stateDeviceLocationOff = viewModel.uiState.value
            assertEquals(LocationStatus.DEVICE_LOCATION_OFF, stateDeviceLocationOff.locationStatus)
            assertFalse(stateDeviceLocationOff.showLocationSettingsButton)
        }

    @Test
    fun uiState_showsCameraSettingsButton_whenCameraIsDeniedOrUnknown() =
        runTest {
            fakePermissionRepository.cameraStatus = CameraPermissionStatus.DENIED

            viewModel = createViewModel()

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
    fun onRefreshPermissions_reevaluatesRepositories() =
        runTest {
            viewModel = createViewModel()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            testScheduler.advanceUntilIdle()

            assertEquals(LocationStatus.READY, viewModel.uiState.value.locationStatus)

            fakeLocationRepository.locationStatus = { LocationStatus.PERMISSION_DENIED }
            viewModel.onRefreshPermissions()
            testScheduler.advanceUntilIdle()

            assertEquals(LocationStatus.PERMISSION_DENIED, viewModel.uiState.value.locationStatus)
            assertTrue(viewModel.uiState.value.showLocationSettingsButton)
        }

    @Test
    fun onDemoModeToggled_updatesDemoMode_whenNoRunIsInProgress() =
        runTest {
            viewModel = createViewModel()

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
            viewModel = createViewModel()

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
