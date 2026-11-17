package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.map.InitializedRunSession
import com.trazo.app.domain.repository.RunSessionRepository
import javax.inject.Inject

class AttachActiveRunSessionUseCase
    @Inject
    constructor(
        private val repository: RunSessionRepository,
    ) {
        operator fun invoke(session: InitializedRunSession) {
            repository.attachActiveSession(session)
        }
    }
