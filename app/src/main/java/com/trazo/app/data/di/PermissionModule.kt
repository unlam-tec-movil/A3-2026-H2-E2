package com.trazo.app.data.di

import com.trazo.app.data.repository.PermissionRepository
import com.trazo.app.data.repository.PermissionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PermissionModule {
    @Binds
    @Singleton
    abstract fun bindPermissionRepository(impl: PermissionRepositoryImpl): PermissionRepository
}
