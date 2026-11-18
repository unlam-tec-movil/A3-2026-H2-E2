package com.trazo.app.data.sensors

import com.trazo.app.domain.model.stops.MotionSample
import com.trazo.app.domain.repository.MotionSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

/**
 * Comparte la lectura del [upstream] entre todos los consumidores: un único listener mientras haya al
 * menos un colector; se da de baja cuando se va el último y vuelve a registrarse si aparece otro.
 * Es el único gestor del ciclo de vida del sensor.
 */
class SharedMotionSource(
    upstream: MotionSource,
    scope: CoroutineScope,
) : MotionSource {
    private val shared: Flow<MotionSample> =
        upstream.samples().shareIn(scope, SharingStarted.WhileSubscribed(), replay = 0)

    override fun samples(): Flow<MotionSample> = shared
}
