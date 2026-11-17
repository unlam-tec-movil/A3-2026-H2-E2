package com.trazo.app.ui.screens.map.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import com.trazo.app.R
import com.trazo.app.domain.model.stops.StopKind
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.TrazoTheme

/**
 * Consulta de una detención de más de 20 s: «Frenada» o «Pausa personal».
 *
 * No se descarta por toque exterior ni por atrás: se cierra al elegir una opción o cuando quien la
 * muestra deja de dibujarla porque la detención terminó. Los botones miden al menos 48 dp de alto y
 * se reordenan en varias líneas cuando crece la fuente.
 */
@Composable
fun StopClassificationDialog(
    onClassify: (StopKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = {},
        modifier = modifier,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = {
            Text(
                text = stringResource(R.string.map_stop_query_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.map_stop_query_text),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onClassify(StopKind.BRAKE) },
                modifier = Modifier.heightIn(min = Sizes.minTouchTarget),
            ) {
                Text(text = stringResource(R.string.map_stop_query_brake))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onClassify(StopKind.PERSONAL_PAUSE) },
                modifier = Modifier.heightIn(min = Sizes.minTouchTarget),
            ) {
                Text(text = stringResource(R.string.map_stop_query_personal_pause))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun StopClassificationDialogPreview() {
    TrazoTheme {
        StopClassificationDialog(onClassify = {})
    }
}
