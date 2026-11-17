package com.trazo.app

import android.app.Application
import android.net.http.HttpResponseCache
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.IOException

@HiltAndroidApp
class TrazoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installHttpCache()
    }

    /** Caché HTTP para los tiles del mapa: menos tráfico a CARTO y zonas recientes sin red. */
    private fun installHttpCache() {
        try {
            HttpResponseCache.install(File(cacheDir, HTTP_CACHE_DIR), HTTP_CACHE_SIZE_BYTES)
        } catch (_: IOException) {
            // Sin caché el mapa sigue funcionando; solo descarga los tiles cada vez.
        }
    }

    private companion object {
        const val HTTP_CACHE_DIR = "map-tiles"
        const val HTTP_CACHE_SIZE_BYTES = 50L * 1024 * 1024
    }
}
