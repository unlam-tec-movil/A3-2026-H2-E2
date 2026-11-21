package com.trazo.app.data.di

import com.trazo.app.data.repository.DemoModeRepository
import com.trazo.app.data.repository.DemoModeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DemoModeModule {
    @Binds
    @Singleton
    abstract fun bindDemoModeRepository(impl: DemoModeRepositoryImpl): DemoModeRepository
}
