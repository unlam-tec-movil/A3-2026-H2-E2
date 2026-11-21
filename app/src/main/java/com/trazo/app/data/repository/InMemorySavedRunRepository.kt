package com.trazo.app.data.repository

import com.trazo.app.domain.model.saved.SavedRun
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Recorridos guardados en memoria. Room lo reemplaza después. */
class InMemorySavedRunRepository
    @Inject
    constructor() : SavedRunRepository {
        private val saved = MutableStateFlow<List<SavedRun>>(emptyList())

        override val runs: Flow<List<SavedRun>> =
            saved.map { list -> list.sortedByDescending { it.startedAt } }

        override suspend fun save(run: SavedRun) {
            saved.update { it + run }
        }
    }
