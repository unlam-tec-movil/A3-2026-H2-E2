package com.trazo.app.ui.components.layout

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.trazo.app.R
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.ui.theme.Spacing

@Composable
fun GoalCelebrationOverlay(
    showCelebration: Boolean,
    goal: RunGoal?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!showCelebration) return

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

                val goalText =
                    when (goal) {
                        RunGoal.THREE_KILOMETERS -> stringResource(R.string.map_goal_3km)
                        RunGoal.FIVE_KILOMETERS -> stringResource(R.string.map_goal_5km)
                        RunGoal.TEN_KILOMETERS -> stringResource(R.string.map_goal_10km)
                        null -> stringResource(R.string.map_goal_generic)
                    }

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

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = Spacing.xl),
                ) {
                    Text(text = stringResource(R.string.map_celebration_button))
                }
            }
        }
    }
}

// PROVISORIO: rememberReduceMotion() es provisorio hasta que Persona 4 publique el suyo.
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    val resolver = context.contentResolver
    return remember(resolver) {
        try {
            Settings.Global.getFloat(
                resolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f,
            ) == 0.0f
        } catch (_: Exception) {
            false
        }
    }
}
