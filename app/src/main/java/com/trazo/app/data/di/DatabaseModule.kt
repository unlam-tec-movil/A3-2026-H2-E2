package com.trazo.app.data.di

import android.content.Context
import androidx.room.Room
import com.trazo.app.data.local.AppDatabase
import com.trazo.app.data.local.SavedRunDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "trazo.db").build()

    @Provides
    fun provideSavedRunDao(database: AppDatabase): SavedRunDao = database.savedRunDao()
}
