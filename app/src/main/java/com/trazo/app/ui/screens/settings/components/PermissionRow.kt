package com.trazo.app.ui.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.trazo.app.R
import com.trazo.app.domain.model.CameraPermissionStatus
import com.trazo.app.domain.model.LocationPermissionState
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.Spacing

@Composable
fun PermissionRow(
    label: String,
    statusText: String,
    showSettingsButton: Boolean,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Sizes.minTouchTarget),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (showSettingsButton) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            OutlinedButton(
                onClick = onSettingsClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = Sizes.minTouchTarget),
            ) {
                Text(
                    text = stringResource(R.string.settings_go_to_settings),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
fun locationPermissionStatusText(state: LocationPermissionState): String =
    when (state) {
        LocationPermissionState.EXACT -> stringResource(R.string.settings_permission_exact)
        LocationPermissionState.APPROXIMATE -> stringResource(R.string.settings_permission_approximate)
        LocationPermissionState.DENIED -> stringResource(R.string.settings_permission_denied)
    }

@Composable
fun cameraPermissionStatusText(status: CameraPermissionStatus): String =
    when (status) {
        CameraPermissionStatus.GRANTED -> stringResource(R.string.settings_permission_granted)
        CameraPermissionStatus.DENIED, CameraPermissionStatus.UNKNOWN -> stringResource(R.string.settings_permission_denied)
    }
