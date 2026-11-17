package com.trazo.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface RunningRunSource {
    /** Id del recorrido mientras la detección aplica (activo, detenido o en pausa personal); null si no. */
    val runningRunId: Flow<String?>
}
