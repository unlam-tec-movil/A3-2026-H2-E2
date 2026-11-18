package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.stops.StopLocation
import com.trazo.app.domain.repository.RunSessionRepository
import com.trazo.app.domain.repository.StopLocationProvider
import javax.inject.Inject

/** Adapta la traza de HU-04: la ubicación de la frenada es el último punto de la sesión activa. */
class RunSessionStopLocationProvider
    @Inject
    constructor(
        private val runSessionRepository: RunSessionRepository,
    ) : StopLocationProvider {
        override fun lastKnown(): StopLocation? =
            (runSessionRepository.activeSession.value as? RunSession.ActiveSession)
                ?.routePoints
                ?.lastOrNull()
                ?.let { StopLocation(it.latitude, it.longitude, it.accuracyMeters) }
    }
