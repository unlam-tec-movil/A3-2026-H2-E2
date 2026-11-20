package com.trazo.app.data.sensors

import com.trazo.app.domain.model.stops.MotionSample
import kotlinx.coroutines.flow.Flow

interface MotionSource {
    /** Flujo frío: registra el sensor al colectar y lo da de baja al cancelar. */
    fun samples(): Flow<MotionSample>
}
