package com.trazo.app.ui.screens.map.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trazo.app.R
import com.trazo.app.ui.theme.Elevation
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme
import com.trazo.app.ui.theme.rememberReduceMotion

internal const val BRAKE_ALERT_ICON_TAG = "brake_alert_icon"

private val BrakeAlertIconCircleSize = 36.dp

/**
 * Alerta de frenada (design-system §6.8): superficie inversa con ícono, título y detalle.
 *
 * Se anuncia a accesibilidad (región en vivo cortés) y comunica con texto e ícono, no solo con color.
 * Entra deslizando y desvaneciéndose; con «Quitar animaciones» aparece sin transición.
 */
@Composable
fun BrakeAlert(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxWidth(),
        enter =
            if (reduceMotion) {
                EnterTransition.None
            } else {
                slideInVertically { fullHeight -> fullHeight } + fadeIn()
            },
        exit =
            if (reduceMotion) {
                ExitTransition.None
            } else {
                slideOutVertically { fullHeight -> fullHeight } + fadeOut()
            },
    ) {
        BrakeAlertSurface(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun BrakeAlertSurface(modifier: Modifier = Modifier) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = Elevation.level3,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(BrakeAlertIconCircleSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_brake),
                    contentDescription = null,
                    modifier = Modifier.testTag(BRAKE_ALERT_ICON_TAG),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.map_brake_alert_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                )
                Text(
                    text = stringResource(R.string.map_brake_alert_detail),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BrakeAlertVisiblePreview() {
    TrazoTheme {
        BrakeAlert(visible = true, modifier = Modifier.padding(Spacing.lg))
    }
}

@Preview(showBackground = true)
@Composable
private fun BrakeAlertHiddenPreview() {
    TrazoTheme {
        BrakeAlert(visible = false, modifier = Modifier.padding(Spacing.lg))
    }
}
