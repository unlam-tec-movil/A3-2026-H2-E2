package com.trazo.app.data.di

import com.trazo.app.data.repository.BrakeRepository
import com.trazo.app.data.repository.InMemoryBrakeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Enlaces de datos de `stop-detection`. El monitor de paradas lo provee `MonitorModule` (di). */
@Module
@InstallIn(SingletonComponent::class)
abstract class StopDetectionModule {
    /** Una sola instancia: las frenadas guardadas sobreviven al cambio de pestaña. */
    @Binds
    @Singleton
    abstract fun bindBrakeRepository(implementation: InMemoryBrakeRepository): BrakeRepository
}
