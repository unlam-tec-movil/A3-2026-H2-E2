package com.trazo.app.ui.screens.profile.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trazo.app.R
import com.trazo.app.ui.components.common.RunValueFormatter
import com.trazo.app.ui.components.common.StatusBadge
import com.trazo.app.ui.components.common.StatusBadgeKind
import com.trazo.app.ui.components.common.labelRes
import com.trazo.app.ui.screens.profile.RunCardUi
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme
import com.trazo.app.ui.theme.tabular

private val CardBorderWidth = 1.dp

/**
 * Card de un recorrido de «Mis recorridos»: título en una sola línea (con «…» si no entra), fecha,
 * marcas y una grilla con distancia, duración, frenadas y regularidad.
 *
 * El lector de pantalla la lee como un solo botón con [RunCardUi.accessibilityLabel], que ya trae
 * todos los datos y las marcas. Toda la card se toca (≥ 48 dp) y avisa con [onClick].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RunCard(
    run: RunCardUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accessibilityLabel = runCardAccessibilityLabel(run)
    val regularityText = RunValueFormatter.formatRegularity(run.regularityPercent)
    Card(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.minTouchTarget)
                .clearAndSetSemantics {
                    contentDescription = accessibilityLabel
                    role = Role.Button
                    onClick {
                        onClick()
                        true
                    }
                },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(width = CardBorderWidth, color = MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = run.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = run.dateText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (run.badges.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    run.badges.forEach { StatusBadge(kind = it) }
                }
            }
            // FlowRow y no una fila de 4 columnas: con la fuente al 200 % las métricas pasan a la línea de abajo.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                RunMetric(label = stringResource(R.string.profile_run_distance_label), value = run.distanceText)
                RunMetric(label = stringResource(R.string.profile_run_time_label), value = run.durationText)
                RunMetric(label = stringResource(R.string.profile_run_brakes_label), value = run.brakeCount.toString())
                RunMetric(label = stringResource(R.string.profile_run_regularity_label), value = regularityText)
            }
        }
    }
}

/**
 * Frase con la que el lector de pantalla lee toda la card: título, fecha, distancia, duración, frenadas y
 * regularidad («sin datos» si no hay), y al final las marcas si las tiene.
 */
@Composable
private fun runCardAccessibilityLabel(run: RunCardUi): String {
    val brakes = pluralStringResource(R.plurals.profile_run_brakes_description, run.brakeCount, run.brakeCount)
    val regularity =
        run.regularityPercent?.let { RunValueFormatter.formatRegularity(it) }
            ?: stringResource(R.string.profile_run_regularity_none_description)
    val base =
        stringResource(
            R.string.profile_run_card_description,
            run.title,
            run.dateText,
            run.distanceText,
            run.durationText,
            brakes,
            regularity,
        )
    if (run.badges.isEmpty()) return base
    val badgeNames = run.badges.map { stringResource(it.labelRes()) }.joinToString(", ")
    return base + " " + stringResource(R.string.profile_run_card_badges_description, badgeNames)
}

/** Valor con su rótulo debajo ("5,2 km" / "Distancia"). Cifras tabulares. */
@Composable
private fun RunMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.tabular(),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val previewRun =
    RunCardUi(
        id = "run-1",
        title = "Vuelta al lago",
        dateText = "Mar 28 oct",
        distanceText = "5,2 km",
        durationText = "00:55:00",
        brakeCount = 3,
        regularityPercent = 80,
        badges = emptyList(),
    )

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RunCardCompletePreview() {
    TrazoTheme { RunCard(run = previewRun, onClick = {}) }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RunCardIncompleteDemoPreview() {
    TrazoTheme {
        RunCard(
            run =
                previewRun.copy(
                    title = "Mi recorrido larguísimo de domingo por la costanera hasta el puerto",
                    regularityPercent = null,
                    badges = listOf(StatusBadgeKind.INCOMPLETE, StatusBadgeKind.DEMO),
                ),
            onClick = {},
        )
    }
}
