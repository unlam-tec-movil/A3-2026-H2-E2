package com.trazo.app.ui.screens.startrun.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trazo.app.R
import com.trazo.app.ui.theme.Elevation
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoShapes
import com.trazo.app.ui.theme.TrazoTheme
import com.trazo.app.ui.theme.rememberReduceMotion

private val IndicatorSize = 24.dp
private const val STATIC_PROGRESS = 0.75f

/**
 * Aviso que se ve en el Mapa mientras Trazo espera una ubicación precisa para comenzar.
 * Con «Quitar animaciones» el indicador queda quieto.
 */
@Composable
fun GpsWaitingNotice(
    goalLabel: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = TrazoShapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level1),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (reduceMotion) {
                CircularProgressIndicator(
                    progress = { STATIC_PROGRESS },
                    modifier = Modifier.size(IndicatorSize),
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(IndicatorSize))
            }

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                Text(
                    text = stringResource(R.string.start_run_waiting_gps),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.start_run_waiting_goal, goalLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.heightIn(min = Sizes.minTouchTarget),
            ) {
                Text(stringResource(R.string.start_run_cancel))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GpsWaitingNoticePreview() {
    TrazoTheme {
        GpsWaitingNotice(goalLabel = "5 km", onCancel = {})
    }
}
