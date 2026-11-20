package com.trazo.app.di

import com.trazo.app.domain.repository.MonotonicClock
import com.trazo.app.domain.repository.MotionSensorAvailability
import com.trazo.app.domain.repository.MotionSource
import com.trazo.app.domain.repository.RunSessionRepository
import com.trazo.app.domain.services.RegularityMonitor
import com.trazo.app.domain.services.SensorStopMonitor
import com.trazo.app.domain.services.StopMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope

/**
 * Monitores de dominio (`domain/services`). Viven fuera de `data/di` porque `data` solo importa
 * `domain.model`. Una sola instancia de cada uno, que corre en el scope de la app y comparte el
 * mismo [MotionSource] (un solo listener del sensor).
 */
@Module
@InstallIn(SingletonComponent::class)
object MonitorModule {
    @Provides
    @Singleton
    fun provideStopMonitor(
        motionSource: MotionSource,
        runSessionRepository: RunSessionRepository,
        clock: MonotonicClock,
        @ApplicationScope scope: CoroutineScope,
    ): StopMonitor = SensorStopMonitor(motionSource, runSessionRepository, clock, scope)

    @Provides
    @Singleton
    fun provideRegularityMonitor(
        motionSource: MotionSource,
        runSessionRepository: RunSessionRepository,
        availability: MotionSensorAvailability,
        clock: MonotonicClock,
        @ApplicationScope scope: CoroutineScope,
    ): RegularityMonitor = RegularityMonitor(motionSource, runSessionRepository, availability, clock, scope)
}
