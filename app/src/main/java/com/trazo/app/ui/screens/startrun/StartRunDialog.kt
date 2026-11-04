package com.trazo.app.ui.screens.startrun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.trazo.app.R
import com.trazo.app.domain.model.RunGoal
import com.trazo.app.ui.components.common.GoalChip
import com.trazo.app.ui.components.common.PrimaryButton
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

/**
 * Modal "Iniciar nuevo recorrido". No guarda estado propio: dibuja lo que recibe y avisa
 * los eventos hacia afuera.
 *
 * @param selectedGoal meta elegida, o `null` si todavía no eligió ninguna.
 * @param onGoalSelected se invoca al tocar una meta.
 * @param onConfirm se invoca al tocar "Comenzar" (solo está habilitado con una meta elegida).
 * @param onDismiss se invoca al tocar "Cancelar" o fuera del modal.
 */
@Composable
fun StartRunDialog(
    selectedGoal: RunGoal?,
    onGoalSelected: (RunGoal) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = { Text(stringResource(R.string.start_run_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(stringResource(R.string.start_run_description))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    RunGoal.entries.forEach { goal ->
                        GoalChip(
                            label = stringResource(R.string.start_run_goal_km, goal.kilometers),
                            selected = goal == selectedGoal,
                            onClick = { onGoalSelected(goal) },
                        )
                    }
                }
                if (selectedGoal == null) {
                    Text(
                        text = stringResource(R.string.start_run_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = Sizes.buttonHeight),
            ) {
                Text(stringResource(R.string.start_run_cancel))
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.start_run_confirm),
                onClick = onConfirm,
                enabled = selectedGoal != null,
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun StartRunDialogNoGoalPreview() {
    TrazoTheme {
        StartRunDialog(
            selectedGoal = null,
            onGoalSelected = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StartRunDialogWithGoalPreview() {
    TrazoTheme {
        StartRunDialog(
            selectedGoal = RunGoal.KM_10,
            onGoalSelected = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}
