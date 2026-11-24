package com.trazo.app.ui.screens.saverun

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.trazo.app.R
import com.trazo.app.domain.model.saved.SavedRun
import com.trazo.app.ui.screens.camera.CameraCaptureScreen
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme
import java.io.File

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

    if (state.showCamera) {
        // La cámara ocupa toda la pantalla; Atrás la cierra sin perder lo escrito.
        BackHandler(onBack = viewModel::onCameraDismiss)
        CameraCaptureScreen(
            onPhotoConfirmed = viewModel::onPhotoConfirmed,
            modifier = modifier.safeDrawingPadding(),
        )
    } else {
        // Botón o gesto de volver del sistema: en vez de salir, pedimos confirmación.
        BackHandler(onBack = viewModel::onBackRequested)

        SaveRunContent(
            state = state,
            onBack = viewModel::onBackRequested,
            onTitleChange = viewModel::onTitleChange,
            onHighlightToggle = viewModel::onHighlightToggle,
            onSave = viewModel::onSave,
            onDiscardConfirm = viewModel::onDiscardConfirm,
            onDiscardDismiss = viewModel::onDiscardDismiss,
            onAddPhoto = viewModel::onAddPhotoClick,
            onPhotoRemove = viewModel::onPhotoRemove,
            modifier = modifier,
        )
    }
}

/** Dibuja el formulario. No conoce el ViewModel: recibe el estado y avisa con funciones. */
@Composable
fun SaveRunContent(
    state: SaveRunUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onHighlightToggle: (String) -> Unit,
    onSave: () -> Unit,
    onDiscardConfirm: () -> Unit,
    onDiscardDismiss: () -> Unit,
    onAddPhoto: () -> Unit,
    onPhotoRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        SaveRunTopBar(onBack = onBack)
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            state.summary?.let { SaveRunSummaryCard(summary = it) }

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.save_run_photos_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text =
                        stringResource(
                            R.string.save_run_photos_counter,
                            state.photoPaths.size,
                            SavedRun.MAX_PHOTOS,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(R.string.save_run_photos_hint, SavedRun.MAX_PHOTOS),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                state.photoPaths.forEachIndexed { index, path ->
                    Box(modifier = Modifier.size(96.dp)) {
                        AsyncImage(
                            model = File(path),
                            contentDescription = stringResource(R.string.save_run_photo_description, index + 1),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                        )
                        FilledTonalIconButton(
                            onClick = { onPhotoRemove(path) },
                            modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.save_run_remove_photo),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = onAddPhoto,
                enabled = state.photoPaths.size < SavedRun.MAX_PHOTOS,
            ) {
                Text(stringResource(R.string.save_run_add_photo))
            }
        }

        if (state.saveError) {
            Text(
                text = stringResource(R.string.save_run_save_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
        }

        // Botón fijo abajo: queda fuera del área que se desplaza.
        Button(
            onClick = onSave,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg)
                    .heightIn(min = Sizes.buttonHeight),
        ) {
            Text(stringResource(R.string.save_run_save))
        }
    }
    if (state.showDiscardDialog) {
        DiscardRunDialog(onConfirm = onDiscardConfirm, onDismiss = onDiscardDismiss)
    }
}

/** Barra superior: flecha para volver y el título de la pantalla. */
@Composable
private fun SaveRunTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.save_run_back),
            )
        }
        Text(
            text = stringResource(R.string.save_run_heading),
            style = MaterialTheme.typography.titleLarge,
        )
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
            onBack = {},
            onTitleChange = {},
            onHighlightToggle = {},
            onSave = {},
            onDiscardConfirm = {},
            onDiscardDismiss = {},
            onAddPhoto = {},
            onPhotoRemove = {},
        )
    }
}
