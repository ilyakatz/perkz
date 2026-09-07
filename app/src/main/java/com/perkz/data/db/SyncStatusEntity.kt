package com.perkz.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_status")
data class SyncStatusEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val lastSyncedAtEpochMillis: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
