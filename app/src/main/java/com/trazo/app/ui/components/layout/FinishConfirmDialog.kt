package com.trazo.app.ui.components.layout

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun FinishConfirmDialog(
    showFinishConfirmDialog: Boolean,
    onConfirmFinish: () -> Unit,
    onCancelFinish: () -> Unit,
) {
    if (!showFinishConfirmDialog) return

    AlertDialog(
        onDismissRequest = onCancelFinish,
        confirmButton = {
            TextButton(
                onClick = onConfirmFinish,
            ) {
                Text("Finalizar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancelFinish,
            ) {
                Text("Cancelar")
            }
        },
    )
}
