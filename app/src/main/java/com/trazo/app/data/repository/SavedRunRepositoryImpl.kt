package com.trazo.app.data.repository

import com.trazo.app.data.local.SavedRunDao
import com.trazo.app.data.local.toDomain
import com.trazo.app.data.local.toEntity
import com.trazo.app.domain.model.saved.SavedRun
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Guarda los recorridos en la base de datos del teléfono (Room). */
class SavedRunRepositoryImpl
    @Inject
    constructor(
        private val dao: SavedRunDao,
    ) : SavedRunRepository {
        override val runs: Flow<List<SavedRun>> =
            dao
                .observeAll()
                .map { entities -> entities.map { it.toDomain() } }
                .flowOn(Dispatchers.Default)

        override suspend fun save(run: SavedRun) {
            dao.insert(run.toEntity())
        }

        override suspend fun getById(id: String): SavedRun? = dao.getById(id)?.toDomain()
    }
