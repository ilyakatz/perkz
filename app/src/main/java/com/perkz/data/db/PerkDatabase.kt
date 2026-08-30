package com.perkz.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PerkEntity::class, UsageEntity::class], version = 6, exportSchema = false)
abstract class PerkDatabase : RoomDatabase() {
    abstract fun perkDao(): PerkDao
}
