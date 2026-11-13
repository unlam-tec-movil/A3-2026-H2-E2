package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.repository.RunSessionRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class GetActiveRunSessionUseCase
    @Inject
    constructor(
        private val repository: RunSessionRepository,
    ) {
        operator fun invoke(): StateFlow<RunSession> = repository.activeSession
    }
