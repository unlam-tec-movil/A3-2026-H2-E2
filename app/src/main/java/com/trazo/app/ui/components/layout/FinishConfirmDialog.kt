package com.trazo.app.ui.components.layout

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.trazo.app.ui.theme.TrazoShapes

@Composable
fun FinishConfirmDialog(
    showFinishConfirmDialog: Boolean,
    onConfirmFinish: () -> Unit,
    onCancelFinish: () -> Unit,
) {
    if (!showFinishConfirmDialog) return

    AlertDialog(
        onDismissRequest = onCancelFinish,
        shape = TrazoShapes.extraLarge,
        confirmButton = {
            TextButton(
                onClick = onConfirmFinish,
            ) {
                Text(
                    text = "Finalizar",
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
                    text = "Cancelar",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
