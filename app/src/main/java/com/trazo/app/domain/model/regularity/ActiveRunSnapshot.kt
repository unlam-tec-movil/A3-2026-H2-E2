package com.trazo.app.domain.model.regularity

import com.trazo.app.domain.model.map.RunState

/** Recorrido en curso: su id y su estado. */
data class ActiveRunSnapshot(
    val runId: String,
    val state: RunState,
)
