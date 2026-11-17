package com.trazo.app.ui.components.layout

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.trazo.app.R
import com.trazo.app.ui.theme.TrazoShapes

@Composable
fun FinishConfirmDialog(
    showFinishConfirmDialog: Boolean,
    goalReached: Boolean,
    onConfirmFinish: () -> Unit,
    onCancelFinish: () -> Unit,
) {
    if (!showFinishConfirmDialog) return

    val messageText =
        if (goalReached) {
            stringResource(R.string.map_finish_confirm_goal_reached)
        } else {
            stringResource(R.string.map_finish_confirm_goal_not_reached)
        }

    AlertDialog(
        onDismissRequest = onCancelFinish,
        text = {
            Text(
                text = messageText,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        shape = TrazoShapes.extraLarge,
        confirmButton = {
            TextButton(
                onClick = onConfirmFinish,
            ) {
                Text(
                    text = stringResource(R.string.map_finish),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancelFinish,
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
