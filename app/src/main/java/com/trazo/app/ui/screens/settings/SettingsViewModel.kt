package com.trazo.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.app.data.repository.DemoModeRepository
import com.trazo.app.data.repository.PermissionRepository
import com.trazo.app.data.repository.RunSessionRepository
import com.trazo.app.domain.model.CameraPermissionStatus
import com.trazo.app.domain.model.LocationPermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val locationPermissionState: LocationPermissionState = LocationPermissionState.DENIED,
    val cameraPermissionStatus: CameraPermissionStatus = CameraPermissionStatus.DENIED,
    val isDemoModeEnabled: Boolean = false,
    val isRunInProgress: Boolean = false,
) {
    val showLocationSettingsButton: Boolean
        get() = locationPermissionState != LocationPermissionState.EXACT

    val showCameraSettingsButton: Boolean
        get() = cameraPermissionStatus != CameraPermissionStatus.GRANTED

    val isDemoModeSwitchEnabled: Boolean
        get() = !isRunInProgress
}

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val permissionRepository: PermissionRepository,
        private val demoModeRepository: DemoModeRepository,
        private val runSessionRepository: RunSessionRepository,
    ) : ViewModel() {
        private val permissionRefreshTrigger = MutableStateFlow(0)

        val uiState: StateFlow<SettingsUiState> =
            combine(
                permissionRefreshTrigger,
                demoModeRepository.isDemoModeEnabled,
                runSessionRepository.isRunInProgress,
            ) { _, isDemoEnabled, isRunInProgress ->
                val locationState = permissionRepository.readLocationPermissionState()
                val cameraStatus = permissionRepository.readCameraPermissionStatus()

                SettingsUiState(
                    locationPermissionState = locationState,
                    cameraPermissionStatus = cameraStatus,
                    isDemoModeEnabled = isDemoEnabled,
                    isRunInProgress = isRunInProgress,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue =
                    SettingsUiState(
                        locationPermissionState = permissionRepository.readLocationPermissionState(),
                        cameraPermissionStatus = permissionRepository.readCameraPermissionStatus(),
                        isDemoModeEnabled = false,
                        isRunInProgress = runSessionRepository.isRunInProgress.value,
                    ),
            )

        fun onRefreshPermissions() {
            permissionRefreshTrigger.value++
        }

        fun onDemoModeToggled(enabled: Boolean) {
            if (runSessionRepository.isRunInProgress.value) {
                return
            }
            viewModelScope.launch {
                demoModeRepository.setDemoModeEnabled(enabled)
            }
        }
    }
