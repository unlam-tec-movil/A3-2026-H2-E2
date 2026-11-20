package com.trazo.app.domain.usecases

import com.trazo.app.domain.model.stops.ClosedStop
import com.trazo.app.domain.model.stops.StopKind
import com.trazo.app.domain.repository.BrakeRepository
import javax.inject.Inject

/** Clasifica una parada terminada y guarda solo las frenadas. Una pausa personal no se guarda. */
class SaveStopUseCase
    @Inject
    constructor(
        private val brakeRepository: BrakeRepository,
    ) {
        operator fun invoke(stop: ClosedStop) {
            if (classify(stop) == StopKind.BRAKE) brakeRepository.save(stop.brake)
        }

        /** Si el corredor no contestó (retomó o terminó el recorrido), la parada cuenta como frenada. */
        fun classify(stop: ClosedStop): StopKind = stop.answer ?: StopKind.BRAKE
    }
