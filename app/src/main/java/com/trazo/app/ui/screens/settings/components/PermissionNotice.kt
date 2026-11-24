package com.trazo.app.ui.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import com.trazo.app.R
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

/**
 * Aviso debajo de los permisos. Sin todos los permisos muestra «Acceso restringido»; con todos
 * concedidos, un mensaje de éxito. En los dos casos sigue el botón «Ir a configuración», para
 * conceder o revocar permisos.
 */
@Composable
fun PermissionNotice(
    arePermissionsGranted: Boolean,
    onOpenSystemSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (arePermissionsGranted) {
            NoticeCard(
                icon = Icons.Filled.CheckCircle,
                containerColor = TrazoTheme.extendedColors.successContainer,
                contentColor = TrazoTheme.extendedColors.onSuccessContainer,
                title = null,
                body = stringResource(R.string.settings_permission_notice_granted),
            )
        } else {
            NoticeCard(
                icon = Icons.Filled.Error,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                title = stringResource(R.string.settings_permission_notice_title),
                body = stringResource(R.string.settings_permission_notice_body),
            )
        }
        OutlinedButton(
            onClick = onOpenSystemSettingsClick,
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

@Composable
private fun NoticeCard(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    title: String?,
    body: String,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Column {
                title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
