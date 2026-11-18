package com.trazo.app.domain.repository

import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopKind
import kotlinx.coroutines.flow.StateFlow

interface StopDetectionRepository {
    val state: StateFlow<StopDetectionState>

    /** No-op si no hay consulta pendiente. */
    fun classifyCurrentStop(kind: StopKind)
}
