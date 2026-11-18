package com.trazo.app.domain.model.stops

data class StopDetectionState(
    val phase: StopPhase = StopPhase.Moving,
    val brakes: List<BrakeEvent> = emptyList(),
) {
    val movement: MovementState
        get() =
            if (phase is StopPhase.Braking) MovementState.BRAKING else MovementState.CONTINUOUS_MOVEMENT

    val pendingClassificationBrakeId: String?
        get() = (phase as? StopPhase.Braking)?.takeIf { it.query == QueryStatus.PENDING }?.brakeId
}
