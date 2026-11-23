package com.trazo.app.data.di

import com.trazo.app.data.location.AndroidLocationClient
import com.trazo.app.data.location.LocationClient
import com.trazo.app.data.repository.LocationRepository
import com.trazo.app.data.repository.LocationRepositoryImpl
import com.trazo.app.data.repository.RunSessionRepository
import com.trazo.app.data.repository.RunSessionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Sesión del recorrido, el GPS que la alimenta y el repositorio de ubicación que usan la UI y los
 * casos de uso: una sola instancia de cada uno para toda la app.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RunSessionModule {
    @Binds
    @Singleton
    abstract fun bindRunSessionRepository(implementation: RunSessionRepositoryImpl): RunSessionRepository

    @Binds
    @Singleton
    abstract fun bindLocationClient(androidLocationClient: AndroidLocationClient): LocationClient

    @Binds
    @Singleton
    abstract fun bindLocationRepository(implementation: LocationRepositoryImpl): LocationRepository
}
