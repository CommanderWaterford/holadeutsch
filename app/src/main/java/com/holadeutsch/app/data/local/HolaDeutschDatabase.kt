package com.holadeutsch.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ProgressEntity::class], version = 2, exportSchema = false)
abstract class HolaDeutschDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
}
