package com.trazo.app.data.di

import com.trazo.app.data.repository.InMemorySavedRunRepository
import com.trazo.app.data.repository.SavedRunRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

/** Enlaza la interfaz del contrato con su implementación en memoria. Room la reemplaza después. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SavedRunModule {
    /** Una sola instancia: los recorridos guardados sobreviven al cambio de pestaña. */
    @Binds
    @Singleton
    abstract fun bindSavedRunRepository(implementation: InMemorySavedRunRepository): SavedRunRepository
}
