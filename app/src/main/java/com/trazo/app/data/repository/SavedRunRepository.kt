package com.trazo.app.data.repository

import com.trazo.app.domain.model.saved.SavedRun
import kotlinx.coroutines.flow.Flow

/** Guarda y expone los recorridos guardados. No decide nada. */
interface SavedRunRepository {
    /** Más reciente primero. */
    val runs: Flow<List<SavedRun>>

    suspend fun save(run: SavedRun)
}
