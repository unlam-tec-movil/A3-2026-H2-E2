package com.trazo.app.domain.model.stops

/** Parada terminada: la frenada candidata y lo que contestó el corredor (`null` si no contestó). */
data class ClosedStop(
    val brake: BrakeEvent,
    val answer: StopKind?,
)
