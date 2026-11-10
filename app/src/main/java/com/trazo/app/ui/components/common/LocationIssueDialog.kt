package com.trazo.app.ui.components.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.trazo.app.R
import com.trazo.app.domain.model.LocationStatus
import com.trazo.app.ui.theme.TrazoTheme

/** Avisa por qué no se puede comenzar el recorrido y ofrece ir a Configuración. */
@Composable
fun LocationIssueDialog(
    issue: LocationStatus,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (titleRes, messageRes) = issue.messageResources()
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = { Text(stringResource(titleRes)) },
        text = { Text(stringResource(messageRes)) },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.location_issue_close))
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.location_issue_open_settings),
                onClick = onOpenSettings,
            )
        },
    )
}

private fun LocationStatus.messageResources(): Pair<Int, Int> =
    when (this) {
        LocationStatus.APPROXIMATE_ONLY ->
            R.string.location_issue_approximate_title to R.string.location_issue_approximate_message
        LocationStatus.DEVICE_LOCATION_OFF ->
            R.string.location_issue_device_off_title to R.string.location_issue_device_off_message
        else ->
            R.string.location_issue_permission_title to R.string.location_issue_permission_message
    }

@Preview(showBackground = true)
@Composable
private fun LocationIssueDialogPreview() {
    TrazoTheme {
        LocationIssueDialog(
            issue = LocationStatus.PERMISSION_DENIED,
            onOpenSettings = {},
            onDismiss = {},
        )
    }
}
