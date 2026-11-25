package com.trazo.app.ui.screens.saverun.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trazo.app.R
import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.ui.components.common.RunValueFormatter
import com.trazo.app.ui.theme.Spacing

/** Tarjeta con el resumen del recorrido que se está por guardar. */
@Composable
fun SaveRunSummaryCard(
    summary: FrozenRunSummary,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text =
                    stringResource(
                        R.string.save_run_summary_date,
                        RunValueFormatter.formatShortDate(summary.startedAt),
                        RunValueFormatter.formatTime(summary.startedAt),
                    ),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SummaryBadge(
                    text =
                        stringResource(
                            if (summary.goalReached) {
                                R.string.save_run_summary_goal_reached
                            } else {
                                R.string.save_run_summary_incomplete
                            },
                        ),
                    highlighted = summary.goalReached,
                )
                if (summary.isDemoMode) {
                    SummaryBadge(text = stringResource(R.string.save_run_summary_demo), highlighted = false)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SummaryMetric(
                    label = stringResource(R.string.save_run_summary_distance),
                    value = RunValueFormatter.formatDistanceKm(summary.metrics.distanceMeters),
                )
                SummaryMetric(
                    label = stringResource(R.string.save_run_summary_time),
                    value = RunValueFormatter.formatDuration(summary.metrics.elapsedTimeSeconds),
                )
                SummaryMetric(
                    label = stringResource(R.string.save_run_summary_brakes),
                    value = summary.brakeCount?.toString() ?: "–",
                )
            }
        }
    }
}

@Composable
private fun SummaryBadge(
    text: String,
    highlighted: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color =
            if (highlighted) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}
