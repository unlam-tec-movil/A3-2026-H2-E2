package com.trazo.app.data.repository

import com.trazo.app.domain.model.saved.SavedRun
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException

/** Repositorio falso en memoria para los tests: guarda los recorridos sin base de datos. */
class FakeSavedRunRepository : SavedRunRepository {
    private val saved = MutableStateFlow<List<SavedRun>>(emptyList())

    override val runs: Flow<List<SavedRun>> =
        saved.map { list -> list.sortedByDescending { it.startedAt } }

    /** Si es verdadero, [save] falla como si no se pudiera escribir en el almacenamiento. */
    var failOnSave: Boolean = false

    override suspend fun save(run: SavedRun) {
        if (failOnSave) throw IOException("No se pudo escribir")
        saved.update { it + run }
    }

    override suspend fun getById(id: String): SavedRun? = saved.value.firstOrNull { it.id == id }
}
