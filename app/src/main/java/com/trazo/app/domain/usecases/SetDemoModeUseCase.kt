package com.trazo.app.domain.usecases

import com.trazo.app.data.repository.DemoModeRepository
import com.trazo.app.data.repository.RunSessionRepository
import javax.inject.Inject

/**
 * Activa o desactiva el modo demo solo si no hay un recorrido en curso.
 * Si hay un recorrido activo, ignora la solicitud para no alterar la carrera.
 */
class SetDemoModeUseCase
    @Inject
    constructor(
        private val demoModeRepository: DemoModeRepository,
        private val runSessionRepository: RunSessionRepository,
    ) {
        suspend operator fun invoke(enabled: Boolean) {
            if (runSessionRepository.isRunInProgress.value) return
            demoModeRepository.setDemoModeEnabled(enabled)
        }
    }
