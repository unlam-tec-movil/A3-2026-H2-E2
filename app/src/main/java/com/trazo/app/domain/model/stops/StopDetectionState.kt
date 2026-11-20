package com.trazo.app.domain.model.stops

data class StopDetectionState(
    val phase: StopPhase = StopPhase.Moving,
) {
    val pendingClassificationBrakeId: String?
        get() = (phase as? StopPhase.Braking)?.takeIf { it.query == QueryStatus.PENDING }?.brake?.id
}
