package com.trazo.app.ui.screens.map.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trazo.app.R
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.ui.components.common.goalLabel
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.rememberReduceMotion

@Composable
fun GoalCelebrationOverlay(
    showCelebration: Boolean,
    goal: RunGoal?,
    onDismiss: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()

    AnimatedVisibility(
        visible = showCelebration,
        enter = if (reduceMotion) EnterTransition.None else fadeIn(),
        exit = if (reduceMotion) ExitTransition.None else fadeOut(),
    ) {
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f))
                    .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(Spacing.xl),
            ) {
                Text(
                    text = stringResource(R.string.map_celebration_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                val goalText = goalLabel(goal?.distanceKm)

                Text(
                    text = stringResource(R.string.map_celebration_reached_goal, goalText),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Spacing.md),
                )

                Text(
                    text = stringResource(R.string.map_celebration_keep_running),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Spacing.sm),
                )

                Column(
                    modifier = Modifier.padding(top = Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Button(onClick = onFinish) {
                        Text(text = stringResource(R.string.map_finish))
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary),
                    ) {
                        Text(text = stringResource(R.string.map_celebration_button))
                    }
                }
            }
        }
    }
}
