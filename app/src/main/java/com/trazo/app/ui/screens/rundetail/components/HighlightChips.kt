package com.trazo.app.ui.screens.rundetail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trazo.app.R
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

private val ChipBorderWidth = 1.dp

/**
 * Sección «Destacados» del detalle: un chip de solo lectura por destacado (no se tocan ni se seleccionan).
 * Los chips pasan a la línea siguiente cuando no entran. Con la lista vacía no compone nada: ni título ni espacio.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HighlightChips(
    highlights: List<String>,
    modifier: Modifier = Modifier,
) {
    if (highlights.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.run_detail_highlights_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            highlights.forEach { HighlightChip(text = it) }
        }
    }
}

@Composable
private fun HighlightChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(width = ChipBorderWidth, color = MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun HighlightChipsPreview() {
    TrazoTheme {
        HighlightChips(
            highlights = listOf("Ritmo parejo", "Sin frenadas largas", "Mejor marca del mes"),
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}
