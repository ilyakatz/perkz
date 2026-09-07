package com.perkz.data.repository

import com.perkz.data.db.PerkDao
import com.perkz.data.db.PerkEntity
import com.perkz.data.db.SyncStatusEntity
import com.perkz.data.db.UsageEntity
import com.perkz.domain.periodKeyFor
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class NotApplicableRepositoryTest {
    @Test
    fun `persisting not applicable clears usage and prevents used fallback`() = runBlocking {
        val dao = RecordingNotApplicableDao()
        val repository = PerkRepository(dao)
        val perk = perk()
        dao.current = UsageEntity(perk.id, periodKeyFor(perk, LocalDate.now()), 25.0)

        assertEquals(
            ToggleSyncResult.LocalOnly,
            repository.setNotApplicable(perk, notApplicable = true)
        )

        assertEquals(
            perk.id to periodKeyFor(perk, LocalDate.now()),
            dao.deleted
        )
        val persisted = dao.inserted.single()
        assertEquals(true, persisted.isNotApplicable)
        assertEquals(false, persisted.usedFromSheet)
        assertEquals(null, persisted.usedAmountFromSheet)
        assertEquals(0.0, repository.currentUsageAmount(persisted), 0.0)
    }

    private fun perk() = PerkEntity(
        id = "perk-1",
        title = "Global entry",
        card = "Amex",
        interval = "Monthly",
        sourceRowNumber = 2,
        resetPeriod = "",
        deadlineTrigger = "",
        maxValueOrUses = "100",
        details = "",
        usedFromSheet = true,
        usedAmountFromSheet = 25.0
    )
}

private class RecordingNotApplicableDao : PerkDao {
    private val perks = MutableStateFlow<List<PerkEntity>>(emptyList())
    private val usage = MutableStateFlow<List<UsageEntity>>(emptyList())
    private val syncStatus = MutableStateFlow<SyncStatusEntity?>(null)
    var current: UsageEntity? = null
    var deleted: Pair<String, String>? = null
    val inserted = mutableListOf<PerkEntity>()

    override fun observePerks(): Flow<List<PerkEntity>> = perks
    override fun observeUsage(): Flow<List<UsageEntity>> = usage
    override fun observeSyncStatus(): Flow<SyncStatusEntity?> = syncStatus
    override suspend fun clearPerks() = perks.emit(emptyList())
    override suspend fun clearUsage() = usage.emit(emptyList())
    override suspend fun insertPerks(items: List<PerkEntity>) {
        inserted += items
        perks.emit(items)
    }
    override suspend fun upsertUsage(usage: UsageEntity) {
        current = usage
    }
    override suspend fun getUsage(perkId: String, periodKey: String): UsageEntity? = current
    override suspend fun deleteUsage(perkId: String, periodKey: String) {
        deleted = perkId to periodKey
        current = null
    }
    override suspend fun updateUsedFromSheet(perkId: String, usedFromSheet: Boolean) = Unit
    override suspend fun upsertSyncStatus(status: SyncStatusEntity) {
        syncStatus.emit(status)
    }
}
