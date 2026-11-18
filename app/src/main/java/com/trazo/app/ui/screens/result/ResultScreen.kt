package com.trazo.app.ui.screens.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.app.R
import com.trazo.app.domain.model.map.RunGoal
import com.trazo.app.ui.screens.result.components.GoalReachedCard
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

/**
 * Pantalla de resultado del recorrido. Conecta el ViewModel; «atrás» no hace nada porque el
 * recorrido todavía no está guardado.
 */
@Composable
fun ResultScreen(
    onContinue: (runId: String) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = true) {}

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ResultEffect.LeaveToMap -> onLeave()
            }
        }
    }

    ResultContent(
        state = state,
        onContinue = { onContinue(viewModel.runId) },
        modifier = modifier,
    )
}

/** Dibuja el resultado: fondo primario, tarjeta centrada y «Continuar» a todo el ancho. */
@Composable
fun ResultContent(
    state: ResultUiState,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isLoaded) {
            GoalReachedCard(
                isGoalReached = state.isGoalReached,
                goal = state.goal,
                distanceText = state.distanceText,
                timeText = state.timeText,
                regularityText = state.regularityText,
            )
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.buttonHeight),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
            ) {
                Text(stringResource(R.string.result_continue))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultContentGoalPreview() {
    TrazoTheme {
        ResultContent(
            state =
                ResultUiState(
                    isLoaded = true,
                    isGoalReached = true,
                    goal = RunGoal.FIVE_KILOMETERS,
                    distanceText = "5,2 km",
                    timeText = "00:32:10",
                ),
            onContinue = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultContentIncompletePreview() {
    TrazoTheme {
        ResultContent(
            state =
                ResultUiState(
                    isLoaded = true,
                    isGoalReached = false,
                    goal = RunGoal.FIVE_KILOMETERS,
                    distanceText = "2,1 km",
                    timeText = "00:14:03",
                    regularityText = "80 %",
                ),
            onContinue = {},
        )
    }
}
