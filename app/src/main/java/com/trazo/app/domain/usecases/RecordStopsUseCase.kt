package com.trazo.app.domain.usecases

import com.trazo.app.domain.repository.StopMonitor
import javax.inject.Inject

/**
 * Escucha las paradas que cierra el [StopMonitor] y le pasa cada una a [SaveStopUseCase], que decide
 * si se guarda. Corre mientras viva la app: no termina por sí solo.
 */
class RecordStopsUseCase
    @Inject
    constructor(
        private val stopMonitor: StopMonitor,
        private val saveStop: SaveStopUseCase,
    ) {
        suspend operator fun invoke() {
            stopMonitor.closedStops.collect { saveStop(it) }
        }
    }
