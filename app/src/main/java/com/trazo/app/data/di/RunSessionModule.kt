package com.trazo.app.data.di

import com.trazo.app.data.location.AndroidLocationClient
import com.trazo.app.data.location.LocationClient
import com.trazo.app.data.repository.RunSessionRepository
import com.trazo.app.data.repository.RunSessionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

/** Sesión del recorrido y el GPS que la alimenta: una sola instancia de cada uno para toda la app. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RunSessionModule {
    @Binds
    @Singleton
    abstract fun bindRunSessionRepository(implementation: RunSessionRepositoryImpl): RunSessionRepository

    @Binds
    @Singleton
    abstract fun bindLocationClient(androidLocationClient: AndroidLocationClient): LocationClient
}
