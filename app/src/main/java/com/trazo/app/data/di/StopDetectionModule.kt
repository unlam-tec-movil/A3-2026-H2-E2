package com.trazo.app.data.di

import com.trazo.app.data.repository.RunSessionRunningRunSource
import com.trazo.app.data.repository.RunSessionStatePublisher
import com.trazo.app.data.repository.RunSessionStopLocationProvider
import com.trazo.app.data.repository.StopDetectionRepositoryImpl
import com.trazo.app.di.ApplicationScope
import com.trazo.app.domain.repository.MonotonicClock
import com.trazo.app.domain.repository.MotionSource
import com.trazo.app.domain.repository.RunStatePublisher
import com.trazo.app.domain.repository.RunningRunSource
import com.trazo.app.domain.repository.StopDetectionRepository
import com.trazo.app.domain.repository.StopLocationProvider
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

    companion object {
        @Provides
        @Singleton
        fun provideStopDetectionRepository(
            motionSource: MotionSource,
            runningRunSource: RunningRunSource,
            locationProvider: StopLocationProvider,
            publisher: RunStatePublisher,
            clock: MonotonicClock,
            @ApplicationScope scope: CoroutineScope,
        ): StopDetectionRepository = StopDetectionRepositoryImpl(motionSource, runningRunSource, locationProvider, publisher, clock, scope)
    }
}
