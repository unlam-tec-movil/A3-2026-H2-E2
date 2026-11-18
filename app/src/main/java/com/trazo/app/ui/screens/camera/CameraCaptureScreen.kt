package com.trazo.app.ui.screens.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
