package com.trazo.app.ui.components.layout

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

/** Radio ecuatorial de la esfera de Web Mercator, en metros. */
private const val EARTH_RADIUS_METERS = 6_378_137.0

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

    data class GeoPoint(
        val latitude: Double,
        val longitude: Double,
    )

    /** Conversión inversa de [normalize]: de coordenadas normalizadas de MapCompose a latitud y longitud. */
    fun denormalize(
        x: Double,
        y: Double,
    ): GeoPoint {
        val longitude = x * 360.0 - 180.0
        val latitude = Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * y))))
        return GeoPoint(latitude = latitude, longitude = longitude)
    }

    /** Metros que cubre un pixel a [latitude] con la [scale] de MapCompose (1.0 en [MAX_ZOOM]). */
    fun metersPerPixel(
        latitude: Double,
        scale: Double,
    ): Double = 2 * PI * EARTH_RADIUS_METERS * cos(Math.toRadians(latitude)) / (FULL_SIZE * scale)

    /** Escala de MapCompose equivalente a un zoom de tiles: 1.0 en [MAX_ZOOM]. */
    fun scaleForZoom(zoom: Int): Double = 2.0.pow(zoom - MAX_ZOOM)

    /** Centro (normalizado) y escala de MapCompose con los que una traza entra completa en el mapa. */
    data class Framing(
        val centerX: Double,
        val centerY: Double,
        val scale: Double,
    )

    /**
     * Encuadre del rectángulo que contiene a [points] en un mapa de [viewportWidthPx] × [viewportHeightPx],
     * dejando [paddingPx] libres en cada borde. La escala la manda el eje más apretado y se limita a los
     * zooms que se muestran ([MIN_VISIBLE_ZOOM] a [MAX_VISIBLE_ZOOM]): un punto solo (o casi) no acerca de más
     * y una traza enorme no aleja de más. Devuelve `null` si no hay puntos.
     */
    fun frameTrace(
        points: List<NormalizedPoint>,
        viewportWidthPx: Int,
        viewportHeightPx: Int,
        paddingPx: Int,
    ): Framing? {
        if (points.isEmpty()) return null

        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }

        val availableWidth = (viewportWidthPx - 2 * paddingPx).coerceAtLeast(1)
        val availableHeight = (viewportHeightPx - 2 * paddingPx).coerceAtLeast(1)
        val spanWidth = (maxX - minX) * FULL_SIZE
        val spanHeight = (maxY - minY) * FULL_SIZE

        // Un eje sin extensión no limita la escala.
        val fitWidth = if (spanWidth > 0.0) availableWidth / spanWidth else Double.POSITIVE_INFINITY
        val fitHeight = if (spanHeight > 0.0) availableHeight / spanHeight else Double.POSITIVE_INFINITY

        return Framing(
            centerX = (minX + maxX) / 2,
            centerY = (minY + maxY) / 2,
            scale = minOf(fitWidth, fitHeight).coerceIn(scaleForZoom(MIN_VISIBLE_ZOOM), scaleForZoom(MAX_VISIBLE_ZOOM)),
        )
    }
}
