package com.trazo.app.ui.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.trazo.app.R
import com.trazo.app.ui.screens.settings.SettingsUiState
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.Spacing

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
                text = stringResource(R.string.settings_demo_mode_label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Switch(
                checked = state.isDemoModeEnabled,
                onCheckedChange = onDemoModeToggle,
                enabled = state.isDemoModeSwitchEnabled,
            )
        }

        noticeText?.let { notice ->
            Spacer(modifier = Modifier.height(Spacing.xs))
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
