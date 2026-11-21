package com.trazo.app.ui.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.trazo.app.R
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoShapes

@Composable
fun FinishConfirmDialog(
    showFinishConfirmDialog: Boolean,
    goalReached: Boolean,
    onConfirmFinish: () -> Unit,
    onCancelFinish: () -> Unit,
    finishFailed: Boolean = false,
    isFinishing: Boolean = false,
) {
    if (!showFinishConfirmDialog) return

    val messageText =
        if (goalReached) {
            stringResource(R.string.map_finish_confirm_goal_reached)
        } else {
            stringResource(R.string.map_finish_confirm_goal_not_reached)
        }

    AlertDialog(
        onDismissRequest = {
            if (!isFinishing) onCancelFinish()
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = messageText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (finishFailed) {
                    Text(
                        text = stringResource(R.string.map_finish_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        shape = TrazoShapes.extraLarge,
        confirmButton = {
            TextButton(
                onClick = onConfirmFinish,
                enabled = !isFinishing,
            ) {
                Text(
                    text = stringResource(if (finishFailed) R.string.map_retry else R.string.map_finish),
                    // Deshabilitado hereda el color de contenido deshabilitado del botón.
                    color = if (isFinishing) Color.Unspecified else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancelFinish,
                enabled = !isFinishing,
            ) {
                Text(
                    text = stringResource(R.string.map_cancel),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
