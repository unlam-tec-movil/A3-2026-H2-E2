package com.trazo.app.data.tiles

import android.util.Log
import com.trazo.app.BuildConfig
import ovh.plrapps.mapcompose.core.TileStreamProvider
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tiles raster CARTO Positron. La plantilla con la API key sale de `CARTO_TILE_URL` en
 * `local.properties` (ver README). Exige la atribución «© OpenStreetMap contributors © CARTO».
 *
 * Usa la caché HTTP que instala [com.trazo.app.TrazoApplication]. Ante cualquier error devuelve
 * null (el tile queda vacío): MapCompose trata las excepciones del proveedor como fatales.
 */
object CartoTileStreamProvider : TileStreamProvider {
    private const val TAG = "CartoTiles"
    private const val TIMEOUT_MILLIS = 10_000
    private const val USER_AGENT = "Trazo-Android"

    private val template: String =
        BuildConfig.CARTO_TILE_URL.also {
            if (it.isBlank()) {
                Log.w(TAG, "Falta CARTO_TILE_URL en local.properties: se usan los tiles públicos.")
            }
        }

    override suspend fun getTileStream(
        row: Int,
        col: Int,
        zoomLvl: Int,
    ): InputStream? =
        try {
            val url = URL(CartoTileUrl.build(template, row, col, zoomLvl))
            val connection =
                (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = TIMEOUT_MILLIS
                    readTimeout = TIMEOUT_MILLIS
                    useCaches = true
                    setRequestProperty("User-Agent", USER_AGENT)
                }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream
            } else {
                connection.disconnect()
                null
            }
        } catch (
            @Suppress("TooGenericExceptionCaught") exception: Exception,
        ) {
            null
        }
}
