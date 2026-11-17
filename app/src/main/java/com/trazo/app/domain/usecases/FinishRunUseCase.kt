package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.map.FrozenRunSummary
import com.trazo.app.domain.repository.RunSessionRepository
import jakarta.inject.Inject

class FinishRunUseCase
    @Inject
    constructor(
        private val repository: RunSessionRepository,
    ) {
        suspend operator fun invoke(): Result<FrozenRunSummary> = repository.finishSession()
    }
