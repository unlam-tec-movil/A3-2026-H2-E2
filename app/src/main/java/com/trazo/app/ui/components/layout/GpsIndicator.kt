package com.trazo.app.ui.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trazo.app.domain.model.map.GpsQuality

@Composable
fun GpsIndicator(
    gpsQuality: GpsQuality,
    modifier: Modifier = Modifier,
) {
    val (label, color) =
        when (gpsQuality) {
            GpsQuality.NO_SIGNAL -> {
                "Sin señal GPS" to MaterialTheme.colorScheme.error
            }

            GpsQuality.LOW_SIGNAL -> {
                "Señal GPS baja" to MaterialTheme.colorScheme.error
            }

            GpsQuality.MEDIUM_SIGNAL -> {
                "Señal GPS media" to MaterialTheme.colorScheme.tertiary
            }

            GpsQuality.FULL_SIGNAL -> {
                "Buena señal GPS" to MaterialTheme.colorScheme.primary
            }
        }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "●",
            color = color,
            style = MaterialTheme.typography.titleMedium,
        )

        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
