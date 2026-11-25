package com.trazo.app.ui.screens.result.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trazo.app.R
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.ui.components.common.goalLabel
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme
import com.trazo.app.ui.theme.rememberReduceMotion
import com.trazo.app.ui.theme.tabular

/**
 * Tarjeta del resultado: «¡Meta cumplida!» con check animado una vez, o «Recorrido finalizado» con la
 * marca «Incompleto». Muestra distancia, tiempo y regularidad («sin dato» si no se pudo medir).
 */
@Composable
fun GoalReachedCard(
    isGoalReached: Boolean,
    goal: RunGoal?,
    distanceText: String,
    timeText: String,
    regularityText: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            if (isGoalReached) {
                AnimatedGoalCheck()
            }

            Text(
                text =
                    stringResource(
                        if (isGoalReached) R.string.result_goal_reached_title else R.string.result_finished_title,
                    ),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )

            if (isGoalReached) {
                Text(
                    text = stringResource(R.string.result_goal_completed, goalLabel(goal?.distanceKm)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                IncompleteBadge()
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                MetricBox(
                    label = stringResource(R.string.result_distance),
                    value = distanceText,
                )
                MetricBox(
                    label = stringResource(R.string.result_time),
                    value = timeText,
                )
                MetricBox(
                    label = stringResource(R.string.result_regularity),
                    value = regularityText ?: stringResource(R.string.run_value_no_data),
                )
            }
        }
    }
}

/** Check que aparece una vez con escala (resorte) y fundido; estático con «Quitar animaciones». */
@Composable
private fun AnimatedGoalCheck() {
    val reduceMotion = rememberReduceMotion()
    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            progress.snapTo(1f)
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            )
        }
    }

    Box(
        modifier =
            Modifier
                .size(CheckCircleSize)
                .scale(progress.value.coerceAtLeast(0f))
                .alpha(progress.value.coerceIn(0f, 1f))
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_goal_check),
            contentDescription = stringResource(R.string.result_goal_check_description),
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(CheckIconSize),
        )
    }
}

/** Marca «Incompleto»: texto y fondo de advertencia, no depende solo del color. */
@Composable
private fun IncompleteBadge() {
    Surface(
        shape = CircleShape,
        color = TrazoTheme.extendedColors.warningContainer,
        contentColor = TrazoTheme.extendedColors.onWarningContainer,
    ) {
        Text(
            text = stringResource(R.string.result_incomplete),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
        )
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .padding(Spacing.lg)
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(end = Spacing.sm),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.tabular(),
            textAlign = TextAlign.End,
        )
    }
}

private val CheckCircleSize = 72.dp
private val CheckIconSize = 40.dp

@Preview(showBackground = true)
@Composable
private fun GoalReachedCardGoalPreview() {
    TrazoTheme {
        GoalReachedCard(
            isGoalReached = true,
            goal = RunGoal.FIVE_KILOMETERS,
            distanceText = "5,2 km",
            timeText = "00:32:10",
            regularityText = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GoalReachedCardIncompletePreview() {
    TrazoTheme {
        GoalReachedCard(
            isGoalReached = false,
            goal = RunGoal.FIVE_KILOMETERS,
            distanceText = "2,1 km",
            timeText = "00:14:03",
            regularityText = "80 %",
        )
    }
}
