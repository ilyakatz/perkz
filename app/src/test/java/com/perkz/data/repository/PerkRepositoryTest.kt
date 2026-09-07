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

class PerkRepositoryTest {
    @Test
    fun `set used amount stores and removes current period usage`() = runBlocking {
        val dao = RecordingPerkDao()
        val repository = PerkRepository(dao)
        val perk = perk()

        assertEquals(ToggleSyncResult.LocalOnly, repository.setUsedAmount(perk, 12.5))
        assertEquals(UsageEntity(perk.id, periodKeyFor(perk, LocalDate.now()), 12.5), dao.upserted)
        assertEquals(true, dao.updatedUsed)

        repository.setUsedAmount(perk, 0.0)
        assertEquals(perk.id to periodKeyFor(perk, LocalDate.now()), dao.deleted)
        assertEquals(false, dao.updatedUsed)
    }

    @Test
    fun `current usage prefers local period usage then sheet values`() = runBlocking {
        val dao = RecordingPerkDao()
        val repository = PerkRepository(dao)
        val perk = perk(usedFromSheet = true, usedAmountFromSheet = 7.0, max = "20")

        dao.current = UsageEntity(perk.id, periodKeyFor(perk, LocalDate.now()), 3.0)
        assertEquals(3.0, repository.currentUsageAmount(perk), 0.0)

        dao.current = null
        assertEquals(7.0, repository.currentUsageAmount(perk), 0.0)
    }

    @Test
    fun `set used converts max value to amount and reports sync mode`() = runBlocking {
        val dao = RecordingPerkDao()
        val repository = PerkRepository(dao)
        val result = repository.setUsed(perk(max = "$25"), checked = true)

        assertEquals(ToggleSyncResult.LocalOnly, result)
        assertEquals(25.0, dao.upserted?.amount ?: 0.0, 0.0)
    }

    private fun perk(
        usedFromSheet: Boolean = false,
        usedAmountFromSheet: Double? = null,
        max: String = ""
    ) = PerkEntity(
        id = "perk-1",
        title = "Test",
        card = "Visa",
        interval = "Monthly",
        sourceRowNumber = 2,
        resetPeriod = "",
        deadlineTrigger = "",
        maxValueOrUses = max,
        details = "",
        usedFromSheet = usedFromSheet,
        usedAmountFromSheet = usedAmountFromSheet
    )
}

private class RecordingPerkDao : PerkDao {
    private val perks = MutableStateFlow<List<PerkEntity>>(emptyList())
    private val usage = MutableStateFlow<List<UsageEntity>>(emptyList())
    private val syncStatus = MutableStateFlow<SyncStatusEntity?>(null)
    var current: UsageEntity? = null
    var upserted: UsageEntity? = null
    var deleted: Pair<String, String>? = null
    var updatedUsed: Boolean? = null

    override fun observePerks(): Flow<List<PerkEntity>> = perks
    override fun observeUsage(): Flow<List<UsageEntity>> = usage
    override fun observeSyncStatus(): Flow<SyncStatusEntity?> = syncStatus
    override suspend fun clearPerks() = perks.emit(emptyList())
    override suspend fun clearUsage() = usage.emit(emptyList())
    override suspend fun insertPerks(items: List<PerkEntity>) = perks.emit(items)
    override suspend fun upsertUsage(usage: UsageEntity) {
        upserted = usage
        current = usage
    }
    override suspend fun getUsage(perkId: String, periodKey: String): UsageEntity? = current
    override suspend fun deleteUsage(perkId: String, periodKey: String) {
        deleted = perkId to periodKey
        current = null
    }
    override suspend fun updateUsedFromSheet(perkId: String, usedFromSheet: Boolean) {
        updatedUsed = usedFromSheet
    }
    override suspend fun upsertSyncStatus(status: SyncStatusEntity) {
        syncStatus.emit(status)
    }
}
