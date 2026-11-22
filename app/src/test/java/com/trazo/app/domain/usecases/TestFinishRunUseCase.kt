package com.trazo.app.domain.usecases

import com.trazo.app.data.repository.InMemoryBrakeRepository
import com.trazo.app.data.repository.RunSessionRepository
import com.trazo.app.data.sensors.MotionSensorAvailability
import com.trazo.app.data.sensors.MotionSource
import com.trazo.app.domain.model.stops.ClosedStop
import com.trazo.app.domain.model.stops.MotionSample
import com.trazo.app.domain.model.stops.StopDetectionState
import com.trazo.app.domain.model.stops.StopKind
import com.trazo.app.domain.services.RegularityMonitor
import com.trazo.app.domain.services.StopMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * [RegularityMonitor] para tests de pantalla: sin muestras del sensor, así que no cierra tramos.
 * Con [sensorAvailable] falso simula un dispositivo sin acelerómetro.
 */
fun testRegularityMonitor(
    runs: RunSessionRepository,
    sensorAvailable: Boolean = true,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
): RegularityMonitor =
    RegularityMonitor(
        motionSource = NoMotionSource,
        runSessionRepository = runs,
        availability = MotionSensorAvailability { sensorAvailable },
        clock = { 0L },
        scope = scope,
    )

/**
 * [FinishRunUseCase] para tests de pantalla: sin frenadas guardadas ni parada abierta, así que
 * finalizar solo delega en [runs]. Para probar el conteo, ver `FinishRunUseCaseTest`.
 */
fun testFinishRunUseCase(
    runs: RunSessionRepository,
    regularityMonitor: RegularityMonitor = testRegularityMonitor(runs),
): FinishRunUseCase =
    FinishRunUseCase(
        runSessionRepository = runs,
        brakeRepository = InMemoryBrakeRepository(),
        stopMonitor = IdleStopMonitor,
        regularityMonitor = regularityMonitor,
    )

private object NoMotionSource : MotionSource {
    override fun samples(): Flow<MotionSample> = emptyFlow()
}

private object IdleStopMonitor : StopMonitor {
    override val state: StateFlow<StopDetectionState> = MutableStateFlow(StopDetectionState())
    override val closedStops: Flow<ClosedStop> = emptyFlow()

    override fun answer(kind: StopKind) = Unit
}
