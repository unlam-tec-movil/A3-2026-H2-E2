package com.trazo.app.data.di

import com.trazo.app.data.repository.InMemoryBrakeRepository
import com.trazo.app.data.repository.SensorStopMonitor
import com.trazo.app.di.ApplicationScope
import com.trazo.app.domain.repository.BrakeRepository
import com.trazo.app.domain.repository.MonotonicClock
import com.trazo.app.domain.repository.MotionSource
import com.trazo.app.domain.repository.RunSessionRepository
import com.trazo.app.domain.repository.StopMonitor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope

/**
 * Enlaces de `stop-detection`. [MotionSource] y [MonotonicClock] los provee únicamente
 * `MotionSensorModule` (floor-regularity); este módulo solo los consume.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class StopDetectionModule {
    /** Una sola instancia: las frenadas guardadas sobreviven al cambio de pestaña. */
    @Binds
    @Singleton
    abstract fun bindBrakeRepository(implementation: InMemoryBrakeRepository): BrakeRepository

    companion object {
        @Provides
        @Singleton
        fun provideStopMonitor(
            motionSource: MotionSource,
            runSessionRepository: RunSessionRepository,
            clock: MonotonicClock,
            @ApplicationScope scope: CoroutineScope,
        ): StopMonitor = SensorStopMonitor(motionSource, runSessionRepository, clock, scope)
    }
}
