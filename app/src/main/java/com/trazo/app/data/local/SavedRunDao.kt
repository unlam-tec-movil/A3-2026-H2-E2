package com.trazo.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedRunDao {
    @Query("SELECT * FROM saved_runs ORDER BY startedAtMillis DESC")
    fun observeAll(): Flow<List<SavedRunEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(run: SavedRunEntity)
}