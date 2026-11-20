package com.trazo.app.data.repository

import com.trazo.app.domain.model.stops.BrakeEvent
import com.trazo.app.domain.repository.BrakeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Frenadas en memoria mientras la app está abierta. Solo recibe y guarda. */
class InMemoryBrakeRepository
    @Inject
    constructor() : BrakeRepository {
        private val _brakes = MutableStateFlow<List<BrakeEvent>>(emptyList())
        override val brakes: StateFlow<List<BrakeEvent>> = _brakes.asStateFlow()

        override fun save(brake: BrakeEvent) {
            _brakes.update { it + brake }
        }
    }
