package com.trazo.app.domain.repository

import com.trazo.app.domain.model.stops.BrakeEvent
import kotlinx.coroutines.flow.StateFlow

/** Guarda las frenadas que recibe. No decide nada: la clasificación la hace `SaveStopUseCase`. */
interface BrakeRepository {
    val brakes: StateFlow<List<BrakeEvent>>

    fun save(brake: BrakeEvent)
}
