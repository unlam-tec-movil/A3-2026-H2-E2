package com.trazo.app.data.di

import com.trazo.app.data.location.AndroidLocationStatusReader
import com.trazo.app.domain.repository.LocationStatusReader
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.time.Clock

@Module
@InstallIn(SingletonComponent::class)
abstract class StartRunModule {
    @Binds
    abstract fun bindLocationStatusReader(implementation: AndroidLocationStatusReader): LocationStatusReader

    companion object {
        /** Fecha y hora reales: se guardan aparte del reloj monotónico (ver `tech.md`). */
        @Provides
        fun provideClock(): Clock = Clock.System
    }
}
