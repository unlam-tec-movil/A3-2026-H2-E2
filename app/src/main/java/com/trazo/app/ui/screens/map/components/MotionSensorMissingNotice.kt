package com.trazo.app.ui.screens.map.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.trazo.app.R
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

internal const val MOTION_SENSOR_MISSING_ICON_TAG = "motion_sensor_missing_icon"

/**
 * Aviso de que el dispositivo no tiene acelerómetro: no se miden frenadas ni regularidad.
 *
 * Comunica con ícono y texto (no solo con color), se anuncia como región en vivo cortés y el texto
 * se ajusta en varias líneas, así que es legible con la fuente al 200 %. Sin estado: lo monta quien
 * corresponda según `RegularityState.sensorAvailable`.
 */
@Composable
fun MotionSensorMissingNotice(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_sensor_off),
                contentDescription = null,
                modifier = Modifier.testTag(MOTION_SENSOR_MISSING_ICON_TAG),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column {
                Text(
                    text = stringResource(R.string.map_motion_sensor_missing_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.map_motion_sensor_missing_detail),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MotionSensorMissingNoticePreview() {
    TrazoTheme {
        MotionSensorMissingNotice(modifier = Modifier.padding(Spacing.lg))
    }
}

@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun MotionSensorMissingNoticeLargeFontPreview() {
    TrazoTheme {
        MotionSensorMissingNotice(modifier = Modifier.padding(Spacing.lg))
    }
}
