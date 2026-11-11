package com.trazo.app.ui.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trazo.app.ui.screens.map.LiveRouteTrackingUiState
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoShapes
import com.trazo.app.ui.theme.tabular

@Composable
fun RunPanel(
    uiState: LiveRouteTrackingUiState,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = TrazoShapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        border = CardDefaults.outlinedCardBorder(),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                text = "Tu recorrido",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = uiState.formattedTime,
                style = MaterialTheme.typography.displayMedium.tabular(),
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetricItem(
                    label = "Distancia",
                    value = uiState.formattedDistance,
                    unit = uiState.distanceUnit,
                )

                MetricItem(
                    label = "Velocidad actual",
                    value = uiState.formattedSpeed,
                    unit = uiState.speedUnit,
                )

                MetricItem(
                    label = "Promedio",
                    value = uiState.formattedAverageSpeed,
                    unit = uiState.speedUnit,
                )
            }
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
