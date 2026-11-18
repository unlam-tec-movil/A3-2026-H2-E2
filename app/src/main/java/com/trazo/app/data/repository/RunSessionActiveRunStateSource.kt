package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.regularity.ActiveRunSnapshot
import com.trazo.app.domain.repository.ActiveRunStateSource
import com.trazo.app.domain.repository.RunSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Adapta la sesión de HU-04 a una instantánea con id y estado. Solo lee; no publica estados. */
class RunSessionActiveRunStateSource
    @Inject
    constructor(
        runSessionRepository: RunSessionRepository,
    ) : ActiveRunStateSource {
        override val activeRun: Flow<ActiveRunSnapshot?> =
            runSessionRepository.activeSession.map { session ->
                when (session) {
                    is RunSession.ActiveSession -> ActiveRunSnapshot(session.id, session.state)
                    RunSession.Idle -> null
                }
            }
    }
