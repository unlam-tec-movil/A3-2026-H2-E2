package com.trazo.app.domain.repository

import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopKind
import kotlinx.coroutines.flow.StateFlow

/** Vigila el sensor durante el recorrido y expone en qué fase de detención está el corredor. */
interface StopMonitor {
    val state: StateFlow<StopDetectionState>

    /** Respuesta del corredor a la consulta de detención. No-op si no hay consulta pendiente. */
    fun answer(kind: StopKind)
}
