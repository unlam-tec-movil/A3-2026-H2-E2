package com.trazo.app.ui.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.trazo.app.R
import com.trazo.app.ui.screens.settings.SettingsUiState
import com.trazo.app.ui.theme.Spacing

/** Card del modo demo con su switch, y debajo el aviso de modo activo o de recorrido en curso. */
@Composable
fun DemoModeSwitch(
    state: SettingsUiState,
    onDemoModeToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val noticeText =
        when {
            state.isRunInProgress -> stringResource(R.string.settings_demo_mode_running_notice)
            state.isDemoModeEnabled -> stringResource(R.string.settings_demo_mode_active_notice)
            else -> null
        }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SettingsGroup {
            SettingsRow(
                icon = Icons.Outlined.Edit,
                label = stringResource(R.string.settings_demo_mode_label),
                subtitle = stringResource(R.string.settings_demo_mode_subtitle),
            ) {
                Switch(
                    checked = state.isDemoModeEnabled,
                    onCheckedChange = onDemoModeToggle,
                    enabled = state.isDemoModeSwitchEnabled,
                )
            }
        }

        noticeText?.let { notice ->
            Text(
                text = notice,
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (state.isRunInProgress) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
            )
        }
    }
}
