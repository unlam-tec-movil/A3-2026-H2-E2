package com.trazo.app.ui.screens.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.app.R
import com.trazo.app.domain.model.CameraPermissionStatus
import com.trazo.app.ui.components.common.PrimaryButton
import com.trazo.app.ui.theme.Spacing

@Composable
fun CameraCaptureContent(
    uiState: CameraCaptureUiState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (uiState.permissionStatus) {
            CameraPermissionStatus.UNKNOWN -> {
                Text(stringResource(R.string.camera_permission_rationale), textAlign = TextAlign.Center)
                Spacer(Modifier.height(Spacing.lg))
                PrimaryButton(
                    text = stringResource(R.string.camera_permission_allow),
                    onClick = onRequestPermission,
                )
            }

            CameraPermissionStatus.DENIED -> {
                Text(stringResource(R.string.camera_permission_denied_message), textAlign = TextAlign.Center)
                Spacer(Modifier.height(Spacing.lg))
                PrimaryButton(
                    text = stringResource(R.string.camera_open_settings),
                    onClick = onRequestPermission,
                )
            }

            CameraPermissionStatus.GRANTED -> {
                Text(stringResource(R.string.camera_ready))
            }
        }
    }
}

@Composable
fun CameraCaptureScreen(
    modifier: Modifier = Modifier,
    viewModel: CameraCaptureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 1) Si el permiso ya estaba dado, no hace falta pedirlo de nuevo.
    LaunchedEffect(Unit) {
        val alreadyGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) viewModel.onPermissionResult(true)
    }

    // 2) El "lanzador" que muestra el cuadro del sistema y devuelve la respuesta.
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onPermissionResult(granted)
        }

    CameraCaptureContent(
        uiState = uiState,
        // 3) El botón "Continuar" lanza el pedido real.
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        // 4) "Ir a configuración" abre los ajustes de nuestra app.
        onOpenSettings = {
            val intent =
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
            context.startActivity(intent)
        },
        modifier = modifier,
    )
}
