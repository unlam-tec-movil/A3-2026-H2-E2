package com.trazo.app.ui.components.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.trazo.app.data.tiles.CartoTileStreamProvider
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.onTouchDown
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState

/** Zoom inicial y centro (Buenos Aires) del mapa antes de que haya algo que encuadrar. */
private const val INITIAL_ZOOM = 12
private const val INITIAL_LATITUDE = -34.6037
private const val INITIAL_LONGITUDE = -58.3816

/**
 * Estado de mapa común de Trazo: tiles de CARTO, zoom inicial y límites de zoom, y cierre al salir de la
 * composición. Lo usan el mapa de Actividad y el del detalle del recorrido guardado. No incluye pin, botones
 * ni trazos: cada mapa agrega los suyos.
 *
 * @param onTouchDown se invoca ante cualquier toque sobre el mapa (Actividad lo usa para dejar de seguir al corredor).
 */
@Composable
fun rememberTrazoMapState(onTouchDown: () -> Unit = {}): MapState {
    val currentOnTouchDown by rememberUpdatedState(onTouchDown)

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
                onTouchDown { currentOnTouchDown() }
            }
        }

    DisposableEffect(mapState) {
        onDispose { mapState.shutdown() }
    }

    return mapState
}
