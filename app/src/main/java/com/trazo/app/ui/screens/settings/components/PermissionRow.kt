package com.trazo.app.ui.screens.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.trazo.app.R
import com.trazo.app.domain.model.CameraPermissionStatus
import com.trazo.app.domain.model.LocationStatus

/**
 * Fila de un permiso con su estado y una flecha. Tocarla abre los ajustes del sistema con [onClick],
 * para conceder o revocar el permiso.
 */
@Composable
fun PermissionRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    statusText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsRow(
        icon = icon,
        label = label,
        subtitle = subtitle,
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun locationPermissionStatusText(status: LocationStatus): String =
    when (status) {
        LocationStatus.READY, LocationStatus.DEVICE_LOCATION_OFF -> stringResource(R.string.settings_permission_exact)
        LocationStatus.APPROXIMATE_ONLY -> stringResource(R.string.settings_permission_approximate)
        LocationStatus.PERMISSION_DENIED -> stringResource(R.string.settings_permission_denied)
    }

@Composable
fun cameraPermissionStatusText(status: CameraPermissionStatus): String =
    when (status) {
        CameraPermissionStatus.GRANTED -> stringResource(R.string.settings_permission_granted)
        CameraPermissionStatus.DENIED, CameraPermissionStatus.UNKNOWN -> stringResource(R.string.settings_permission_denied)
    }
