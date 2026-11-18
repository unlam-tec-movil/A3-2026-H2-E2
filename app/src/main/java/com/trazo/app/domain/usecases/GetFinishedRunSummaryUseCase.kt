package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.repository.RunSessionRepository
import jakarta.inject.Inject

class GetFinishedRunSummaryUseCase
    @Inject
    constructor(
        private val repository: RunSessionRepository,
    ) {
        // Solo lee: el repositorio no tiene `remove`, así que este caso de uso nunca limpia el resumen.
        operator fun invoke(id: String): FrozenRunSummary? = repository.getFinishedSummary(id)
    }
