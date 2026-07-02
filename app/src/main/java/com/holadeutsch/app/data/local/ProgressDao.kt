package com.holadeutsch.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Query("SELECT * FROM progress")
    fun observeAll(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress")
    suspend fun getAllOnce(): List<ProgressEntity>

    @Query("SELECT * FROM progress WHERE wordId = :wordId")
    suspend fun get(wordId: Int): ProgressEntity?

    @Upsert
    suspend fun upsert(progress: ProgressEntity)

    @Query("DELETE FROM progress")
    suspend fun clearAll()
}
