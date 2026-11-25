package com.trazo.app.ui.screens.map.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trazo.app.R
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.domain.model.map.RunState
import com.trazo.app.ui.components.common.goalLabel
import com.trazo.app.ui.screens.map.LiveRouteTrackingUiState
import com.trazo.app.ui.theme.Elevation
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoShapes
import com.trazo.app.ui.theme.TrazoTheme
import com.trazo.app.ui.theme.tabular

/** Marca de estado (referencia §6.4): 24 dp de alto y 10 dp de padding horizontal. */
private val BadgeHeight = 24.dp
private val BadgeHorizontalPadding = 10.dp

/**
 * Panel del recorrido: encabezado con "Tiempo recorrido" y la meta, temporizador con los
 * [controls] a su derecha, y métricas debajo. No conoce acciones: los controles llegan por slot.
 */
@Composable
fun RunPanel(
    uiState: LiveRouteTrackingUiState,
    modifier: Modifier = Modifier,
    controls: @Composable RowScope.() -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = TrazoShapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = Elevation.level1,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.map_elapsed_time),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                uiState.goal?.let { goal -> GoalBadge(goal = goal) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = uiState.formattedTime,
                    style = MaterialTheme.typography.displayMedium.tabular(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )

                controls()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetricItem(
                    label = stringResource(R.string.map_distance),
                    value = uiState.formattedDistance,
                    unit = stringResource(R.string.map_unit_km),
                )

                MetricItem(
                    label = stringResource(R.string.map_current_speed),
                    value = uiState.formattedSpeed,
                    unit = stringResource(R.string.map_unit_kmh),
                )

                MetricItem(
                    label = stringResource(R.string.map_average_speed),
                    value = uiState.formattedAverageSpeed,
                    unit = stringResource(R.string.map_unit_kmh),
                )
            }
        }
    }
}

@Composable
private fun GoalBadge(goal: RunGoal) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(
            modifier =
                Modifier
                    .height(BadgeHeight)
                    .padding(horizontal = BadgeHorizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.map_goal_badge, goalLabel(goal.distanceKm)),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    unit: String,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.tabular(),
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = unit,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RunPanelPreview() {
    val state =
        LiveRouteTrackingUiState(
            hasActiveRun = true,
            runState = RunState.ACTIVE,
            goal = RunGoal.FIVE_KILOMETERS,
            formattedTime = "00:06:42",
            areControlActionsAllowed = true,
        )
    TrazoTheme {
        RunPanel(uiState = state) {
            RunControls(uiState = state, onPause = {}, onResume = {}, onFinish = {})
        }
    }
}
