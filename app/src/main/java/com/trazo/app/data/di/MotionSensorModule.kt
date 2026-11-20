package com.trazo.app.data.di

import android.os.SystemClock
import com.trazo.app.data.repository.RunSessionActiveRunStateSource
import com.trazo.app.data.repository.SensorRegularityMonitor
import com.trazo.app.data.sensors.AccelerometerMotionSource
import com.trazo.app.data.sensors.AndroidMotionSensorAvailability
import com.trazo.app.data.sensors.SharedMotionSource
import com.trazo.app.di.ApplicationScope
import com.trazo.app.domain.repository.ActiveRunStateSource
import com.trazo.app.domain.repository.MonotonicClock
import com.trazo.app.domain.repository.MotionSensorAvailability
import com.trazo.app.domain.repository.MotionSource
import com.trazo.app.domain.repository.RegularityMonitor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope

/**
 * Único proveedor de [MotionSource] (compartido) y de [MonotonicClock] para toda la app: tanto
 * `stop-detection` como la regularidad reciben el mismo flujo, con un solo listener del sensor.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MotionSensorModule {
    @Binds
    @Singleton
    abstract fun bindMotionSensorAvailability(implementation: AndroidMotionSensorAvailability): MotionSensorAvailability

    @Binds
    @Singleton
    abstract fun bindActiveRunStateSource(implementation: RunSessionActiveRunStateSource): ActiveRunStateSource

    companion object {
        @Provides
        @Singleton
        fun provideMotionSource(
            accelerometer: AccelerometerMotionSource,
            @ApplicationScope scope: CoroutineScope,
        ): MotionSource = SharedMotionSource(accelerometer, scope)

        @Provides
        @Singleton
        fun provideMonotonicClock(): MonotonicClock = MonotonicClock { SystemClock.elapsedRealtime() }

        @Provides
        @Singleton
        fun provideRegularityMonitor(
            motionSource: MotionSource,
            activeRunStateSource: ActiveRunStateSource,
            availability: MotionSensorAvailability,
            clock: MonotonicClock,
            @ApplicationScope scope: CoroutineScope,
        ): RegularityMonitor = SensorRegularityMonitor(motionSource, activeRunStateSource, availability, clock, scope)
    }
}
