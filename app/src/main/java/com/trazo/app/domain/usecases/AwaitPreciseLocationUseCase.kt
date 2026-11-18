package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.map.RawLocationPoint
import com.trazo.app.domain.repository.LocationClient
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Espera la primera ubicación con precisión de [MAX_ACCURACY_METERS] o menos. Al cancelar la
 * corrutina se deja de pedir ubicación. Si el flujo falla (por ejemplo, sin permiso), propaga el
 * error.
 */
class AwaitPreciseLocationUseCase
    @Inject
    constructor(
        private val locationClient: LocationClient,
    ) {
        suspend operator fun invoke(): RawLocationPoint =
            locationClient
                .getLocationUpdates(INTERVAL_MILLIS)
                .first { it.accuracyMeters <= MAX_ACCURACY_METERS }

        companion object {
            const val MAX_ACCURACY_METERS = 30f
            const val INTERVAL_MILLIS = 1_000L
        }
    }
