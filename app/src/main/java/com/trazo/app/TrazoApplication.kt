package com.trazo.app

import android.app.Application
import android.net.http.HttpResponseCache
import com.trazo.app.di.ApplicationScope
import com.trazo.app.domain.usecases.RecordStopsUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltAndroidApp
class TrazoApplication : Application() {
    @Inject
    lateinit var recordStops: RecordStopsUseCase

    @Inject
    @field:ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        installHttpCache()
        startRecordingStops()
    }

    /** Guarda las paradas que se cierran; arranca antes de cualquier recorrido para no perder ninguna. */
    private fun startRecordingStops() {
        applicationScope.launch { recordStops() }
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
