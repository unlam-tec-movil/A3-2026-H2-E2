package com.trazo.app.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.trazo.app.R
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.makePathDataBuilder
import ovh.plrapps.mapcompose.api.removePath
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.ui.MapUI

private const val TRACE_OUTLINE_PATH_ID = "trace-outline"
private const val TRACE_PATH_ID = "trace"

/**
 * Mapa de solo lectura del detalle de un recorrido guardado: dibuja la traza completa, encuadrada con su
 * rectángulo. No tiene pin, botones ni sigue al corredor; solo muestra la atribución del proveedor de tiles.
 *
 * Con menos de 2 puntos no hay traza que dibujar: muestra [R.string.run_detail_map_unavailable] en su lugar,
 * para que el resto del detalle siga a la vista (requisito 3.9).
 *
 * @param contentDescription resumen del recorrido para el lector de pantalla: el mapa no es legible con él.
 */
@Composable
fun RunTraceMap(
    routePoints: List<RoutePoint>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    if (routePoints.size < 2) {
        TraceUnavailable(modifier = modifier)
        return
    }

    val mapState = rememberTrazoMapState()
    val traceColor = TrazoTheme.extendedColors.traceLive
    val traceOutlineColor = TrazoTheme.extendedColors.traceLiveOutline
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val framingPaddingPx = with(LocalDensity.current) { Spacing.xl.roundToPx() }

    LaunchedEffect(routePoints) {
        mapState.removePath(TRACE_OUTLINE_PATH_ID)
        mapState.removePath(TRACE_PATH_ID)
        val pathData =
            mapState
                .makePathDataBuilder()
                .addPoints(
                    routePoints.map { point ->
                        val normalized = WebMercator.normalize(point.latitude, point.longitude)
                        normalized.x to normalized.y
                    },
                ).build() ?: return@LaunchedEffect

        mapState.addPath(
            id = TRACE_OUTLINE_PATH_ID,
            pathData = pathData,
            width = Sizes.traceLiveWidth + 2.dp,
            color = traceOutlineColor,
            zIndex = 0f,
        )
        mapState.addPath(
            id = TRACE_PATH_ID,
            pathData = pathData,
            width = Sizes.traceLiveWidth,
            color = traceColor,
            zIndex = 1f,
        )
    }

    // Encuadra cuando cambia la traza o, recién medido, el tamaño del mapa.
    LaunchedEffect(routePoints, viewport) {
        if (viewport == IntSize.Zero) return@LaunchedEffect
        val framing =
            WebMercator.frameTrace(
                points = routePoints.map { WebMercator.normalize(it.latitude, it.longitude) },
                viewportWidthPx = viewport.width,
                viewportHeightPx = viewport.height,
                paddingPx = framingPaddingPx,
            ) ?: return@LaunchedEffect
        mapState.scrollTo(framing.centerX, framing.centerY, framing.scale)
    }

    Box(
        modifier =
            modifier
                .semantics { this.contentDescription = contentDescription }
                .onSizeChanged { viewport = it },
    ) {
        MapUI(
            modifier = Modifier.fillMaxSize(),
            state = mapState,
        )
        MapAttribution(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(Spacing.sm),
        )
    }
}

/** Texto alternativo cuando la traza no se puede mostrar. */
@Composable
private fun TraceUnavailable(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.run_detail_map_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 240)
@Composable
private fun RunTraceMapUnavailablePreview() {
    TrazoTheme {
        RunTraceMap(routePoints = emptyList(), contentDescription = "", modifier = Modifier.fillMaxSize())
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 240)
@Composable
private fun RunTraceMapWithTracePreview() {
    val route =
        listOf(
            -34.600 to -58.400,
            -34.610 to -58.385,
            -34.625 to -58.390,
            -34.640 to -58.365,
        ).map { (latitude, longitude) ->
            RoutePoint(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = 5f,
                speedMetersPerSecond = 2.5f,
                monotonicTimestampMillis = 0L,
            )
        }
    TrazoTheme {
        RunTraceMap(routePoints = route, contentDescription = "Traza de prueba", modifier = Modifier.fillMaxSize())
    }
}
