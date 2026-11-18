package com.trazo.app.ui.screens.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.trazo.app.R
import com.trazo.app.domain.model.CameraPermissionStatus
import com.trazo.app.ui.components.common.CameraPreview
import com.trazo.app.ui.components.common.PrimaryButton
import com.trazo.app.ui.theme.Spacing
import java.io.File

@Composable
fun CameraCaptureContent(
    uiState: CameraCaptureUiState,
    imageCapture: ImageCapture,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onTakePhoto: () -> Unit,
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
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
                    onClick = onOpenSettings,
                )
            }

            CameraPermissionStatus.GRANTED -> {
                val photoPath = uiState.photoPath
                if (photoPath == null) {
                    CameraPreview(
                        imageCapture = imageCapture,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    if (uiState.hasCaptureError) {
                        Spacer(Modifier.height(Spacing.sm))
                        Text(stringResource(R.string.camera_capture_error), textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(Spacing.lg))
                    PrimaryButton(
                        text = stringResource(R.string.camera_take_photo),
                        onClick = onTakePhoto,
                    )
                } else {
                    AsyncImage(
                        model = File(photoPath),
                        contentDescription = stringResource(R.string.camera_photo_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Spacing.lg))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedButton(onClick = onRetake) {
                            Text(stringResource(R.string.camera_retake))
                        }
                        PrimaryButton(
                            text = stringResource(R.string.camera_use_photo),
                            onClick = onConfirm,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraCaptureScreen(
    modifier: Modifier = Modifier,
    onPhotoConfirmed: (String) -> Unit = {},
    viewModel: CameraCaptureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val imageCapture = remember { ImageCapture.Builder().build() }

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

    // 3) Saca la foto con CameraX, la guarda en un archivo y avisa al ViewModel.
    val takePhoto: () -> Unit = {
        val folder = File(context.filesDir, "photos").apply { mkdirs() }
        val file = File(folder, "trazo_${System.currentTimeMillis()}.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewModel.onPhotoCaptured(file.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    viewModel.onCaptureError()
                }
            },
        )
    }

    CameraCaptureContent(
        uiState = uiState,
        imageCapture = imageCapture,
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onOpenSettings = {
            val intent =
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
            context.startActivity(intent)
        },
        onTakePhoto = takePhoto,
        onRetake = viewModel::onRetake,
        // 4) "Usar foto": el ViewModel devuelve la ruta y se la entregamos a quien use la pantalla.
        onConfirm = { viewModel.onConfirm()?.let(onPhotoConfirmed) },
        modifier = modifier,
    )
}
