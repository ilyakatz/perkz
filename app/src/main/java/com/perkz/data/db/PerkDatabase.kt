package com.perkz.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PerkEntity::class, UsageEntity::class, SyncStatusEntity::class],
    version = 12,
    exportSchema = false
)
abstract class PerkDatabase : RoomDatabase() {
    abstract fun perkDao(): PerkDao

    companion object {
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_status (
                        id INTEGER NOT NULL,
                        lastSyncedAtEpochMillis INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE perks ADD COLUMN benefitUnit TEXT NOT NULL DEFAULT 'auto'")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sync_status ADD COLUMN rawHeadersJson TEXT NOT NULL DEFAULT '[]'")
            }
        }
    }
}
