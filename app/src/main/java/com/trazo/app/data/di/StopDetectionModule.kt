package com.trazo.app.data.di

import com.trazo.app.data.repository.InMemoryBrakeRepository
import com.trazo.app.data.repository.RunSessionRunningRunSource
import com.trazo.app.data.repository.RunSessionStatePublisher
import com.trazo.app.data.repository.RunSessionStopLocationProvider
import com.trazo.app.data.repository.SensorStopMonitor
import com.trazo.app.di.ApplicationScope
import com.trazo.app.domain.repository.BrakeRepository
import com.trazo.app.domain.repository.MonotonicClock
import com.trazo.app.domain.repository.MotionSource
import com.trazo.app.domain.repository.RunStatePublisher
import com.trazo.app.domain.repository.RunningRunSource
import com.trazo.app.domain.repository.StopLocationProvider
import com.trazo.app.domain.repository.StopMonitor
import com.trazo.app.domain.usecases.SaveStopUseCase
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
    @Binds
    abstract fun bindRunningRunSource(implementation: RunSessionRunningRunSource): RunningRunSource

    @Binds
    abstract fun bindStopLocationProvider(implementation: RunSessionStopLocationProvider): StopLocationProvider

    @Binds
    abstract fun bindRunStatePublisher(implementation: RunSessionStatePublisher): RunStatePublisher

    /** Una sola instancia: las frenadas guardadas sobreviven al cambio de pestaña. */
    @Binds
    @Singleton
    abstract fun bindBrakeRepository(implementation: InMemoryBrakeRepository): BrakeRepository

    companion object {
        @Provides
        @Singleton
        fun provideStopMonitor(
            motionSource: MotionSource,
            runningRunSource: RunningRunSource,
            locationProvider: StopLocationProvider,
            publisher: RunStatePublisher,
            clock: MonotonicClock,
            saveStop: SaveStopUseCase,
            @ApplicationScope scope: CoroutineScope,
        ): StopMonitor = SensorStopMonitor(motionSource, runningRunSource, locationProvider, publisher, clock, saveStop, scope)
    }
}
