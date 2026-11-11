package com.trazo.app.ui.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.trazo.app.domain.model.map.GpsQuality
import com.trazo.app.ui.theme.TrazoTheme

@Composable
fun GpsIndicator(
    gpsQuality: GpsQuality,
    modifier: Modifier = Modifier,
) {
    val (label, color) =
        when (gpsQuality) {
            GpsQuality.NO_SIGNAL -> {
                "Sin señal GPS" to TrazoTheme.extendedColors.warning
            }

            GpsQuality.WEAK_SIGNAL -> {
                "Señal GPS débil" to TrazoTheme.extendedColors.warning
            }

            GpsQuality.FULL_SIGNAL -> {
                "Buena señal GPS" to TrazoTheme.extendedColors.success
            }
        }

    Row(
        modifier =
            modifier.semantics(mergeDescendants = true) {
                contentDescription = label
            },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "●",
            color = color,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.clearAndSetSemantics { },
        )

        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
