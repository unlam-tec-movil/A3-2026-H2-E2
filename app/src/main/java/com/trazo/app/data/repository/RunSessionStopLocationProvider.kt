package com.trazo.app.data.repository

import com.trazo.app.domain.model.map.RunSession
import com.trazo.app.domain.model.stops.StopLocation
import com.trazo.app.domain.repository.RunSessionRepository
import com.trazo.app.domain.repository.StopLocationProvider
import javax.inject.Inject
import javax.inject.Provider

/**
 * Adapta la traza de HU-04: la ubicación de la frenada es el último punto de la sesión activa. Recibe
 * un `Provider` para no formar un ciclo de Hilt con `RunSessionRepositoryImpl`.
 */
class RunSessionStopLocationProvider
    @Inject
    constructor(
        private val runSessionRepository: Provider<RunSessionRepository>,
    ) : StopLocationProvider {
        override fun lastKnown(): StopLocation? =
            (runSessionRepository.get().activeSession.value as? RunSession.ActiveSession)
                ?.routePoints
                ?.lastOrNull()
                ?.let { StopLocation(it.latitude, it.longitude, it.accuracyMeters) }
    }
