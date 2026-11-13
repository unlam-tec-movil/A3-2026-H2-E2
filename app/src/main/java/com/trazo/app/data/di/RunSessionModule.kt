package com.trazo.app.data.di

import com.trazo.app.data.repository.RunSessionRepositoryImpl
import com.trazo.app.domain.repository.RunSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RunSessionModule {
    @Binds
    @Singleton
    abstract fun bindRunSessionRepository(implementation: RunSessionRepositoryImpl): RunSessionRepository
}
