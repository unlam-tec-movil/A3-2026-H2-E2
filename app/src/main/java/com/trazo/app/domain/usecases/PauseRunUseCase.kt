package com.trazo.app.domain.usecases

import com.trazo.app.domain.repository.RunSessionRepository
import jakarta.inject.Inject

class PauseRunUseCase
    @Inject
    constructor(
        private val repository: RunSessionRepository,
    ) {
        suspend operator fun invoke(): Result<Unit> = repository.pauseSession()
    }
