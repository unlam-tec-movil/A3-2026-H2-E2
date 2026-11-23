package com.trazo.app.ui.screens.saverun

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.app.R
import com.trazo.app.domain.model.saved.SavedRun
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

/** Pantalla «Guardar recorrido»: conecta el ViewModel y avisa cuando hay que salir. */
@Composable
fun SaveRunScreen(
    onSaved: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SaveRunViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SaveRunEffect.Saved -> onSaved()
                SaveRunEffect.Close -> onClose()
            }
        }
    }

    // Botón o gesto de volver del sistema: en vez de salir, pedimos confirmación.
    BackHandler(onBack = viewModel::onBackRequested)

    SaveRunContent(
        state = state,
        onTitleChange = viewModel::onTitleChange,
        onHighlightToggle = viewModel::onHighlightToggle,
        onSave = viewModel::onSave,
        onDiscardConfirm = viewModel::onDiscardConfirm,
        onDiscardDismiss = viewModel::onDiscardDismiss,
        modifier = modifier,
    )
}

/** Dibuja el formulario. No conoce el ViewModel: recibe el estado y avisa con funciones. */
@Composable
fun SaveRunContent(
    state: SaveRunUiState,
    onTitleChange: (String) -> Unit,
    onHighlightToggle: (String) -> Unit,
    onSave: () -> Unit,
    onDiscardConfirm: () -> Unit,
    onDiscardDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        Text(
            text = stringResource(R.string.save_run_heading),
            style = MaterialTheme.typography.headlineMedium,
        )
        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.save_run_title_label)) },
            isError = state.titleError,
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(
                            if (state.titleError) R.string.save_run_title_error else R.string.save_run_title_helper,
                        ),
                    )
                    Text(
                        stringResource(
                            R.string.save_run_title_counter,
                            state.title.length,
                            SavedRun.TITLE_MAX_LENGTH,
                        ),
                    )
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        val highlightOptions =
            listOf(
                stringResource(R.string.save_run_highlight_even_floor),
                stringResource(R.string.save_run_highlight_safe),
                stringResource(R.string.save_run_highlight_lit),
                stringResource(R.string.save_run_highlight_shade),
            )
        Text(
            text = stringResource(R.string.save_run_highlights_title),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            highlightOptions.forEach { option ->
                val selected = option in state.highlights
                FilterChip(
                    selected = selected,
                    onClick = { onHighlightToggle(option) },
                    label = { Text(option) },
                    leadingIcon =
                        if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        },
                )
            }
        }

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.buttonHeight),
        ) {
            Text(stringResource(R.string.save_run_save))
        }
    }
    if (state.showDiscardDialog) {
        DiscardRunDialog(onConfirm = onDiscardConfirm, onDismiss = onDiscardDismiss)
    }
}

/** Pregunta si se descarta el recorrido. «Descartar» usa el estilo destructivo (rojo). */
@Composable
private fun DiscardRunDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_run_discard_title)) },
        text = { Text(stringResource(R.string.save_run_discard_text)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) {
                Text(stringResource(R.string.save_run_discard_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.save_run_discard_keep))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SaveRunContentPreview() {
    TrazoTheme {
        SaveRunContent(
            state = SaveRunUiState(title = "Mi carrera", highlights = setOf("Piso parejo")),
            onTitleChange = {},
            onHighlightToggle = {},
            onSave = {},
            onDiscardConfirm = {},
            onDiscardDismiss = {},
        )
    }
}
