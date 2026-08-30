package com.perkz.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PerkDao {
    @Query("SELECT * FROM perks ORDER BY interval, card, title")
    fun observePerks(): Flow<List<PerkEntity>>

    @Query("SELECT * FROM usage")
    fun observeUsage(): Flow<List<UsageEntity>>

    @Query("DELETE FROM perks")
    suspend fun clearPerks()

    @Query("DELETE FROM usage")
    suspend fun clearUsage()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerks(items: List<PerkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsage(usage: UsageEntity)

    @Query("DELETE FROM usage WHERE perkId = :perkId AND periodKey = :periodKey")
    suspend fun deleteUsage(perkId: String, periodKey: String)

    @Query("UPDATE perks SET usedFromSheet = :usedFromSheet WHERE id = :perkId")
    suspend fun updateUsedFromSheet(perkId: String, usedFromSheet: Boolean)
}
