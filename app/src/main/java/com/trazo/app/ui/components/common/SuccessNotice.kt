package com.trazo.app.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

private val NoticeIconSize = 20.dp
private val NoticeVerticalPadding = 14.dp

/**
 * Aviso de éxito en línea (referencia §6.7): fondo de éxito (Lima), ícono de check y el [text].
 * El lector de pantalla lo anuncia sin interrumpir lo que se está leyendo (live region polite).
 */
@Composable
fun SuccessNotice(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = TrazoTheme.extendedColors.successContainer,
        contentColor = TrazoTheme.extendedColors.onSuccessContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = NoticeVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(NoticeIconSize),
            )
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SuccessNoticePreview() {
    TrazoTheme {
        SuccessNotice(text = "Recorrido guardado.")
    }
}
