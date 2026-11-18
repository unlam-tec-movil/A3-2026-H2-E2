package com.trazo.app.ui.screens.camera

import com.trazo.app.domain.model.CameraPermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CameraCaptureViewModelTest {
    private lateinit var viewModel: CameraCaptureViewModel

    @Before
    fun setUp() {
        viewModel = CameraCaptureViewModel()
    }

    @Test
    fun `estado inicial no tiene permiso definido, foto ni error`() {
        val state = viewModel.uiState.value
        assertEquals(CameraPermissionStatus.UNKNOWN, state.permissionStatus)
        assertNull(state.photoPath)
        assertFalse(state.hasCaptureError)
    }

    @Test
    fun `permiso concedido guarda GRANTED`() {
        viewModel.onPermissionResult(granted = true)
        assertEquals(CameraPermissionStatus.GRANTED, viewModel.uiState.value.permissionStatus)
    }

    @Test
    fun `foto capturada guarda la ruta`() {
        viewModel.onPhotoCaptured("/fotos/a.jpg")
        assertEquals("/fotos/a.jpg", viewModel.uiState.value.photoPath)
    }

    @Test
    fun `permiso rechazado guarda DENIED`() {
        viewModel.onPermissionResult(granted = false)
        assertEquals(CameraPermissionStatus.DENIED, viewModel.uiState.value.permissionStatus)
    }

    @Test
    fun `error captura y se borra con una foto nueva`() {
        viewModel.onCaptureError()
        assertTrue(viewModel.uiState.value.hasCaptureError)
        viewModel.onPhotoCaptured("/x.jpg")
        assertFalse(viewModel.uiState.value.hasCaptureError)
    }

    @Test
    fun `repetir descarta la foto y mantiene el permiso`() {
        viewModel.onPermissionResult(true)
        viewModel.onPhotoCaptured("/x.jpg")
        viewModel.onRetake()
        assertNull(viewModel.uiState.value.photoPath)
        assertEquals(CameraPermissionStatus.GRANTED, viewModel.uiState.value.permissionStatus)
    }

    @Test
    fun `confirmar sin foto devuelve null`() {
        val result = viewModel.onConfirm()
        assertNull(result)
    }

    @Test
    fun `confirmar con foto devuelve la ruta y reinicia estado`() {
        viewModel.onPhotoCaptured("/x.jpg")
        val result = viewModel.onConfirm()
        assertEquals("/x.jpg", result)
        assertNull(viewModel.uiState.value.photoPath)
        assertEquals(CameraPermissionStatus.UNKNOWN, viewModel.uiState.value.permissionStatus)
    }
}
