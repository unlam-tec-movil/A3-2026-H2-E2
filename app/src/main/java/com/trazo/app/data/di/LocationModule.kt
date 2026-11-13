package com.trazo.app.data.di

import com.trazo.app.data.location.AndroidLocationClient
import com.trazo.app.domain.repository.LocationClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {
    @Binds
    @Singleton
    abstract fun bindLocationClient(androidLocationClient: AndroidLocationClient): LocationClient
}
