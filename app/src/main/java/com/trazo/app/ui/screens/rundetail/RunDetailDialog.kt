package com.trazo.app.ui.screens.rundetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.trazo.app.R
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.ui.components.common.StatusBadge
import com.trazo.app.ui.components.common.StatusBadgeKind
import com.trazo.app.ui.components.layout.RunTraceMap
import com.trazo.app.ui.screens.rundetail.components.HighlightChips
import com.trazo.app.ui.screens.rundetail.components.RunPhotos
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme
import com.trazo.app.ui.theme.tabular

private val TraceMapHeight = 240.dp

/**
 * Modal de detalle de un recorrido guardado, a pantalla completa. Es un `Dialog` y no una hoja inferior porque
 * el mapa recibe gestos de arrastre que chocarían con el gesto de cierre de la hoja. No agrega animaciones
 * propias: usa la animación del sistema del diálogo, que respeta «Quitar animaciones».
 *
 * @param onDismiss se invoca con el botón «Cerrar» o con «Atrás».
 */
@Composable
fun RunDetailDialog(
    detail: RunDetailUi,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        RunDetailContent(detail = detail, onDismiss = onDismiss)
    }
}

/**
 * Contenido del detalle. Se dibuja solo a partir de [detail]. Arriba, fija, la barra con el título y «Cerrar»;
 * debajo, con scroll vertical, el mapa de la traza, la velocidad promedio, la meta, las marcas y, solo si no
 * están vacíos, los destacados y las fotos.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RunDetailContent(
    detail: RunDetailUi,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(title = detail.title, onDismiss = onDismiss)

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                RunTraceMap(
                    routePoints = detail.routePoints,
                    contentDescription = detail.mapDescription,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(TraceMapHeight),
                )

                Column {
                    DetailStat(label = stringResource(R.string.run_detail_average_speed_label), value = detail.averageSpeedText)
                    Text(
                        text = detail.goalText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.heightIn(min = Sizes.minTouchTarget),
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    detail.badges.forEach { StatusBadge(kind = it) }
                }

                HighlightChips(highlights = detail.highlights)
                RunPhotos(photoPaths = detail.photoPaths)
            }
        }
    }
}

/** Barra superior fija: título (hasta 2 líneas) y el botón «Cerrar» de 48 dp. */
@Composable
private fun DetailTopBar(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = Spacing.lg, end = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .weight(1f)
                    .semantics { heading() },
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(Sizes.minTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.run_detail_close),
            )
        }
    }
}

/** Rótulo a la izquierda y valor a la derecha, con cifras tabulares. */
@Composable
private fun DetailStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.minTouchTarget),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.tabular(),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val previewTrace =
    listOf(-34.600 to -58.400, -34.615 to -58.385, -34.640 to -58.365).map { (latitude, longitude) ->
        RoutePoint(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = 5f,
            speedMetersPerSecond = 2.5f,
            monotonicTimestampMillis = 0L,
        )
    }

private val previewDetail =
    RunDetailUi(
        id = "a",
        title = "Vuelta al lago",
        averageSpeedText = "9,5 km/h",
        goalText = "Meta 10 km",
        badges = listOf(StatusBadgeKind.GOAL_REACHED),
        highlights = listOf("Ritmo parejo", "Sin frenadas largas"),
        photoPaths = listOf("/foto-1.jpg", "/foto-2.jpg"),
        routePoints = previewTrace,
        mapDescription = "Mapa con la traza del recorrido Vuelta al lago.",
    )

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun RunDetailContentCompletePreview() {
    TrazoTheme { RunDetailContent(detail = previewDetail, onDismiss = {}) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun RunDetailContentWithoutHighlightsNorPhotosPreview() {
    TrazoTheme {
        RunDetailContent(
            detail =
                previewDetail.copy(
                    badges = listOf(StatusBadgeKind.INCOMPLETE, StatusBadgeKind.DEMO),
                    highlights = emptyList(),
                    photoPaths = emptyList(),
                ),
            onDismiss = {},
        )
    }
}
