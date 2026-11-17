package com.trazo.app.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.trazo.app.R
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.ui.theme.Elevation
import com.trazo.app.ui.theme.Sizes
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.hasMarker
import ovh.plrapps.mapcompose.api.hasPath
import ovh.plrapps.mapcompose.api.makePathDataBuilder
import ovh.plrapps.mapcompose.api.moveMarker
import ovh.plrapps.mapcompose.api.onTouchDown
import ovh.plrapps.mapcompose.api.removePath
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.updatePath
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState

private const val RUNNER_MARKER_ID = "runner"
private const val ROUTE_OUTLINE_PATH_ID = "route-outline"
private const val ROUTE_PATH_ID = "route"

/** Zoom al centrar en el corredor y zoom inicial (Buenos Aires) antes del primer fix. */
private const val RUNNER_ZOOM = 17
private const val INITIAL_ZOOM = 12
private const val INITIAL_LATITUDE = -34.6037
private const val INITIAL_LONGITUDE = -58.3816

/** Pin de posición (referencia §6.12): Primary950, borde blanco de 3 dp y halo al 35 %. */
private val RunnerPinSize = 18.dp
private val RunnerHaloSize = 40.dp
private val RunnerPinBorder = 3.dp
private const val RUNNER_HALO_ALPHA = 0.35f
private const val ATTRIBUTION_BACKGROUND_ALPHA = 0.8f

@Composable
fun MapLayer(
    routePoints: List<RoutePoint>,
    currentLocation: RoutePoint?,
    isMapCenteredOnRunner: Boolean,
    onCenterMapClicked: () -> Unit,
    onMapDragged: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContent: @Composable () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val currentOnMapDragged by rememberUpdatedState(onMapDragged)
    val runnerDescription = stringResource(R.string.map_your_location)

    val mapState =
        remember {
            val initial = WebMercator.normalize(INITIAL_LATITUDE, INITIAL_LONGITUDE)
            MapState(
                levelCount = WebMercator.LEVEL_COUNT,
                fullWidth = WebMercator.FULL_SIZE,
                fullHeight = WebMercator.FULL_SIZE,
                tileSize = WebMercator.TILE_SIZE,
            ) {
                scroll(initial.x, initial.y)
                scale(WebMercator.scaleForZoom(INITIAL_ZOOM))
                // Límites de zoom: nunca se piden tiles que CARTO no sirve.
                maxScale(WebMercator.scaleForZoom(WebMercator.MAX_VISIBLE_ZOOM))
                minimumScaleMode(Forced(WebMercator.scaleForZoom(WebMercator.MIN_VISIBLE_ZOOM)))
            }.apply {
                addLayer(CartoTileStreamProvider)
                // Cualquier toque del corredor sobre el mapa deja de seguir su posición.
                onTouchDown { currentOnMapDragged() }
            }
        }

    DisposableEffect(mapState) {
        onDispose { mapState.shutdown() }
    }

    var hasCenteredInitially by remember { mutableStateOf(false) }

    LaunchedEffect(currentLocation, isMapCenteredOnRunner) {
        val location = currentLocation ?: return@LaunchedEffect
        val point = WebMercator.normalize(location.latitude, location.longitude)

        if (mapState.hasMarker(RUNNER_MARKER_ID)) {
            mapState.moveMarker(RUNNER_MARKER_ID, point.x, point.y)
        } else {
            mapState.addMarker(
                id = RUNNER_MARKER_ID,
                x = point.x,
                y = point.y,
                relativeOffset = Offset(-0.5f, -0.5f),
                clickable = false,
                zIndex = 1f,
            ) {
                RunnerPin(description = runnerDescription)
            }
        }

        if (!hasCenteredInitially) {
            mapState.scrollTo(point.x, point.y, WebMercator.scaleForZoom(RUNNER_ZOOM))
            hasCenteredInitially = true
        } else if (isMapCenteredOnRunner) {
            mapState.scrollTo(point.x, point.y)
        }
    }

    val traceColor = TrazoTheme.extendedColors.traceLive
    val traceOutlineColor = TrazoTheme.extendedColors.traceLiveOutline

    LaunchedEffect(routePoints) {
        if (routePoints.size < 2) {
            mapState.removePath(ROUTE_OUTLINE_PATH_ID)
            mapState.removePath(ROUTE_PATH_ID)
            return@LaunchedEffect
        }

        val pathData =
            mapState
                .makePathDataBuilder()
                .addPoints(
                    routePoints.map { point ->
                        val normalized = WebMercator.normalize(point.latitude, point.longitude)
                        normalized.x to normalized.y
                    },
                ).build() ?: return@LaunchedEffect

        if (mapState.hasPath(ROUTE_PATH_ID)) {
            mapState.updatePath(ROUTE_OUTLINE_PATH_ID, pathData = pathData)
            mapState.updatePath(ROUTE_PATH_ID, pathData = pathData)
        } else {
            mapState.addPath(
                id = ROUTE_OUTLINE_PATH_ID,
                pathData = pathData,
                width = Sizes.traceLiveWidth + 2.dp,
                color = traceOutlineColor,
                zIndex = 0f,
            )
            mapState.addPath(
                id = ROUTE_PATH_ID,
                pathData = pathData,
                width = Sizes.traceLiveWidth,
                color = traceColor,
                zIndex = 1f,
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        MapUI(
            modifier = Modifier.fillMaxSize(),
            state = mapState,
        )

        // Atribución y botón «Centrar» 16 dp por encima del panel del recorrido (§6.2).
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(Spacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = stringResource(R.string.map_attribution),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .weight(1f, fill = false)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = ATTRIBUTION_BACKGROUND_ALPHA),
                                shape = MaterialTheme.shapes.extraSmall,
                            ).padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
                )

                Spacer(modifier = Modifier.weight(1f))

                SmallFloatingActionButton(
                    onClick = {
                        onCenterMapClicked()
                        currentLocation?.let { location ->
                            val point = WebMercator.normalize(location.latitude, location.longitude)
                            scope.launch {
                                mapState.scrollTo(
                                    point.x,
                                    point.y,
                                    WebMercator.scaleForZoom(RUNNER_ZOOM),
                                )
                            }
                        }
                    },
                    modifier = Modifier.size(Sizes.minTouchTarget),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    elevation =
                        FloatingActionButtonDefaults.elevation(
                            defaultElevation = Elevation.level2,
                            pressedElevation = Elevation.level2,
                            focusedElevation = Elevation.level2,
                            hoveredElevation = Elevation.level2,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = stringResource(R.string.map_recenter),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            bottomContent()
        }
    }
}

@Composable
private fun RunnerPin(description: String) {
    Box(
        modifier =
            Modifier
                .size(RunnerHaloSize)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(RunnerHaloSize)
                    .background(
                        color = MaterialTheme.colorScheme.inversePrimary.copy(alpha = RUNNER_HALO_ALPHA),
                        shape = CircleShape,
                    ),
        )
        Box(
            modifier =
                Modifier
                    .size(RunnerPinSize)
                    .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                    .border(width = RunnerPinBorder, color = MaterialTheme.colorScheme.onPrimary, shape = CircleShape),
        )
    }
}
