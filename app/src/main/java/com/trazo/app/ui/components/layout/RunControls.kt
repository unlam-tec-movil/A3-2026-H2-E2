package com.trazo.app.ui.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.trazo.app.R
import com.trazo.app.ui.screens.map.LiveRouteTrackingUiState
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoShapes

@Composable
fun RunControls(
    uiState: LiveRouteTrackingUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Button(
            onClick = {
                if (uiState.isRunning) {
                    onPause()
                } else if (uiState.isPaused) {
                    onResume()
                }
            },
            enabled = uiState.canPause || uiState.canResume,
            shape = TrazoShapes.medium,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(Sizes.runControl),
        ) {
            Text(
                text = if (uiState.isPaused) stringResource(R.string.map_resume) else stringResource(R.string.map_pause),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        OutlinedButton(
            onClick = onFinish,
            enabled = uiState.canFinish,
            shape = TrazoShapes.medium,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(Sizes.runControl),
        ) {
            Text(
                text = stringResource(R.string.map_finish),
                color = if (uiState.canFinish) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
