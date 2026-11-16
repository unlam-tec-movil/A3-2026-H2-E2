package com.trazo.app.ui.components.layout

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/**
 * Proyección Web Mercator (la de los tiles de CARTO/OSM) a las coordenadas normalizadas
 * de MapCompose: x e y van de 0 a 1, con (0, 0) arriba a la izquierda.
 */
object WebMercator {
    /**
     * Nivel de tile más profundo que se pide (MapCompose usa niveles 0..[MAX_ZOOM]). Desde el 17,
     * CARTO dibuja las alturas de cada cuadra y casi no muestra nombres de calles; desde el 19
     * responde 403 y el mapa queda en blanco.
     */
    const val MAX_ZOOM = 16
    const val LEVEL_COUNT = MAX_ZOOM + 1

    /** Alejamiento máximo: la ciudad, no el planeta. */
    const val MIN_VISIBLE_ZOOM = 10

    /** Acercamiento máximo: los tiles de [MAX_ZOOM] agrandados ×2 (siguen nítidos por ser `@2x`). */
    const val MAX_VISIBLE_ZOOM = MAX_ZOOM + 1

    /** Tiles `@2x` de 512 px, nítidos en pantallas de alta densidad. */
    const val TILE_SIZE = 512

    /** Ancho y alto del mundo en el nivel [MAX_ZOOM] (33.554.432 px). */
    const val FULL_SIZE = TILE_SIZE shl MAX_ZOOM

    /** Límite de latitud de Web Mercator: más allá, la proyección tiende a infinito. */
    const val MAX_LATITUDE = 85.05112878

    data class NormalizedPoint(
        val x: Double,
        val y: Double,
    )

    fun normalize(
        latitude: Double,
        longitude: Double,
    ): NormalizedPoint {
        val lat = latitude.coerceIn(-MAX_LATITUDE, MAX_LATITUDE)
        val lon = longitude.coerceIn(-180.0, 180.0)
        val latRad = Math.toRadians(lat)

        val x = (lon + 180.0) / 360.0
        val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0

        return NormalizedPoint(x = x.coerceIn(0.0, 1.0), y = y.coerceIn(0.0, 1.0))
    }

    /** Escala de MapCompose equivalente a un zoom de tiles: 1.0 en [MAX_ZOOM]. */
    fun scaleForZoom(zoom: Int): Double = 2.0.pow(zoom - MAX_ZOOM)
}
