package com.trazo.app.domain.repository

import com.trazo.app.domain.model.regularity.ActiveRunSnapshot
import kotlinx.coroutines.flow.Flow

interface ActiveRunStateSource {
    /** Recorrido en curso con su estado; `null` si no hay (sesión inactiva). */
    val activeRun: Flow<ActiveRunSnapshot?>
}
