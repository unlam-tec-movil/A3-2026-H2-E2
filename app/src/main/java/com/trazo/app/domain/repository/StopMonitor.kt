package com.trazo.app.domain.repository

import com.trazo.app.domain.model.stops.ClosedStop
import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Vigila el sensor durante el recorrido y expone en qué fase de detención está el corredor. */
interface StopMonitor {
    val state: StateFlow<StopDetectionState>

    /**
     * Paradas terminadas, con lo que contestó el corredor. Cada parada se emite una sola vez y no se
     * repite a quien se suscribe después: quien las guarda tiene que estar suscripto antes de que
     * empiece el recorrido (lo lanza `TrazoApplication` al arrancar).
     */
    val closedStops: Flow<ClosedStop>

    /** Respuesta del corredor a la consulta de detención. No-op si no hay consulta pendiente. */
    fun answer(kind: StopKind)
}
