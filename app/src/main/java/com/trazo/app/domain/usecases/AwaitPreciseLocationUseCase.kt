package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.map.GpsSignalRules
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.domain.model.map.isAccurate
import com.trazo.app.domain.repository.LocationClient
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Espera la primera ubicación precisa según [GpsSignalRules]. Al cancelar la corrutina se deja
 * de pedir ubicación. Si el flujo falla (por ejemplo, sin permiso), propaga el error.
 */
class AwaitPreciseLocationUseCase
    @Inject
    constructor(
        private val locationClient: LocationClient,
    ) {
        suspend operator fun invoke(): RoutePoint =
            locationClient
                .getLocationUpdates(INTERVAL_MILLIS)
                .first { it.isAccurate() }

        companion object {
            const val INTERVAL_MILLIS = 1_000L
        }
    }
