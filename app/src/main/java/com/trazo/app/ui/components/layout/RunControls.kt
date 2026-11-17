package com.trazo.app.ui.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.trazo.app.R
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.ui.screens.map.LiveRouteTrackingUiState
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoShapes
import com.trazo.app.ui.theme.TrazoTheme

/** Opacidades de Material 3 para un contenedor y un contenido deshabilitados. */
private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTENT_ALPHA = 0.38f

/**
 * Controles del recorrido en curso: Pausar/Reanudar y Finalizar, como botones de ícono de
 * 56 × 56 dp (referencia §6.2). Sin recorrido en curso se muestran deshabilitados.
 */
@Composable
fun RunControls(
    uiState: LiveRouteTrackingUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        val isPaused = uiState.isPaused

        RunControlButton(
            onClick = if (isPaused) onResume else onPause,
            enabled = uiState.canPause || uiState.canResume,
            colors =
                if (isPaused) {
                    runControlColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    runControlColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                },
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription =
                    stringResource(if (isPaused) R.string.map_resume else R.string.map_pause),
            )
        }

        RunControlButton(
            onClick = onFinish,
            enabled = uiState.canFinish,
            colors =
                runControlColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
        ) {
            Icon(
                imageVector = Icons.Rounded.Stop,
                contentDescription = stringResource(R.string.map_finish),
            )
        }
    }
}

@Composable
private fun RunControlButton(
    onClick: () -> Unit,
    enabled: Boolean,
    colors: IconButtonColors,
    content: @Composable () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = TrazoShapes.medium,
        colors = colors,
        modifier = Modifier.size(Sizes.runControl),
        content = content,
    )
}

@Composable
private fun runControlColors(
    containerColor: Color,
    contentColor: Color,
): IconButtonColors {
    val onSurface = MaterialTheme.colorScheme.onSurface
    return IconButtonDefaults.filledIconButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA),
        disabledContentColor = onSurface.copy(alpha = DISABLED_CONTENT_ALPHA),
    )
}

@Preview(showBackground = true)
@Composable
private fun RunControlsPreview() {
    TrazoTheme {
        RunControls(
            uiState =
                LiveRouteTrackingUiState(
                    hasActiveRun = true,
                    runState = RunState.ACTIVE,
                    areControlActionsAllowed = true,
                ),
            onPause = {},
            onResume = {},
            onFinish = {},
        )
    }
}
