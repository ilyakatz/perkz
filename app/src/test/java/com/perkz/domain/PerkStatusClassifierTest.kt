package com.perkz.domain

import com.perkz.data.db.PerkEntity
import com.perkz.ui.model.PerkStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PerkStatusClassifierTest {
    private val today = LocalDate.of(2026, 9, 6)

    @Test
    fun `amount at max is already used`() {
        assertStatus(max = "100", amount = 100.0, expected = PerkStatus.Used)
    }

    @Test
    fun `amount over max is already used`() {
        assertStatus(max = "100", amount = 125.0, expected = PerkStatus.Used)
    }

    @Test
    fun `expired takes precedence over uncompleted usage`() {
        assertStatus(deadline = "September 5", expected = PerkStatus.Expired)
    }

    @Test
    fun `positive amount below max is partially used`() {
        assertStatus(max = "$100", amount = 25.0, expected = PerkStatus.PartiallyUsed)
    }

    @Test
    fun `near deadline is expiring soon`() {
        assertStatus(deadline = "September 10", expected = PerkStatus.ExpiringSoon)
    }

    @Test
    fun `current monthly period with no usage needs use`() {
        assertStatus(interval = "Monthly", expected = PerkStatus.NeedsUse)
    }

    @Test
    fun `future reset month is upcoming`() {
        assertStatus(resetPeriod = "October", expected = PerkStatus.Upcoming)
    }

    @Test
    fun `local usage takes precedence over sheet amount`() {
        val perk = perk(
            max = "100",
            usedFromSheet = true,
            usedAmountFromSheet = 20.0
        )

        assertEquals(75.0, usageAmountFor(perk, 75.0), 0.0)
    }

    @Test
    fun `sheet numeric usage is used when local usage is absent`() {
        val perk = perk(usedAmountFromSheet = 20.0)

        assertEquals(20.0, usageAmountFor(perk, null), 0.0)
    }

    @Test
    fun `sheet used flag falls back to parsed max amount`() {
        val perk = perk(max = "$25", usedFromSheet = true)

        assertEquals(25.0, usageAmountFor(perk, null), 0.0)
    }

    @Test
    fun `sheet used flag falls back to one for nonnumeric max`() {
        val perk = perk(max = "one use", usedFromSheet = true)

        assertEquals(1.0, usageAmountFor(perk, null), 0.0)
    }

    private fun assertStatus(
        max: String = "",
        interval: String = "Monthly",
        resetPeriod: String = "",
        deadline: String = "",
        amount: Double = 0.0,
        expected: PerkStatus
    ) {
        assertEquals(
            expected,
            classifyPerkStatus(
                perk(
                    interval = interval,
                    resetPeriod = resetPeriod,
                    deadline = deadline,
                    max = max
                ),
                today,
                amount
            )
        )
    }

    private fun perk(
        interval: String = "Monthly",
        resetPeriod: String = "",
        deadline: String = "",
        max: String = "",
        usedFromSheet: Boolean = false,
        usedAmountFromSheet: Double? = null
    ) = PerkEntity(
        id = "test",
        title = "Test perk",
        card = "Test card",
        interval = interval,
        sourceRowNumber = 1,
        resetPeriod = resetPeriod,
        deadlineTrigger = deadline,
        maxValueOrUses = max,
        details = "",
        usedFromSheet = usedFromSheet,
        usedAmountFromSheet = usedAmountFromSheet
    )
}
