package com.trazo.app.data.di

import android.os.SystemClock
import com.trazo.app.data.clock.MonotonicClock
import com.trazo.app.data.sensors.AccelerometerMotionSource
import com.trazo.app.data.sensors.AndroidMotionSensorAvailability
import com.trazo.app.data.sensors.MotionSensorAvailability
import com.trazo.app.data.sensors.MotionSource
import com.trazo.app.data.sensors.SharedMotionSource
import com.trazo.app.di.ApplicationScope
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
    }
}
