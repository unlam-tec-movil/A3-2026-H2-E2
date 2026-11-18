package com.trazo.app.ui.screens.camera

import androidx.lifecycle.ViewModel
import com.trazo.app.domain.model.CameraPermissionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class CameraCaptureUiState(
    val permissionStatus: CameraPermissionStatus = CameraPermissionStatus.UNKNOWN,
    val photoPath: String? = null,
    val hasCaptureError: Boolean = false,
)

@HiltViewModel
class CameraCaptureViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(CameraCaptureUiState())
        val uiState: StateFlow<CameraCaptureUiState> = _uiState.asStateFlow()

        // El sistema respondió al pedido de permiso: guardar GRANTED o DENIED.
        fun onPermissionResult(granted: Boolean) {
            _uiState.update {
                it.copy(
                    permissionStatus = if (granted) CameraPermissionStatus.GRANTED else CameraPermissionStatus.DENIED,
                )
            }
        }

        // CameraX sacó la foto: guardar la ruta y borrar cualquier error anterior.
        fun onPhotoCaptured(path: String) {
            _uiState.update {
                it.copy(
                    photoPath = path,
                    hasCaptureError = false,
                )
            }
        }

        // CameraX falló al sacar la foto: marcar el error.
        fun onCaptureError() {
            _uiState.update {
                it.copy(
                    hasCaptureError = true,
                )
            }
        }

        // "Repetir": descartar la foto y el error, mantener el permiso.
        fun onRetake() {
            _uiState.update {
                it.copy(
                    photoPath = null,
                    hasCaptureError = false,
                )
            }
        }

        // "Usar foto": devolver la ruta (o null si no hay foto) y volver al estado inicial.
        fun onConfirm(): String? {
            val path = _uiState.value.photoPath ?: return null
            _uiState.value = CameraCaptureUiState()
            return path
        }
    }
