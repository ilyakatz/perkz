package com.perkz.domain

import com.perkz.data.db.PerkEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerkPeriodUtilsTest {
    private val date = LocalDate.of(2026, 9, 6)

    @Test
    fun `builds keys for common interval types`() {
        assertEquals("2026-09", periodKeyFor(perk(interval = "Monthly"), date))
        assertEquals("2026-Q3", periodKeyFor(perk(interval = "Quarterly"), date))
        assertEquals("2026-H2", periodKeyFor(perk(interval = "Semi-Annual"), date))
        assertEquals("2026", periodKeyFor(perk(interval = "Annual"), date))
        assertEquals("2026-09", periodKeyFor(perk(interval = "Unknown"), date))
    }

    @Test
    fun `reset period ranges take precedence over month names`() {
        assertEquals("2026-H1", periodKeyFor(perk(resetPeriod = "January - June"), date))
        assertEquals("2026-H2", periodKeyFor(perk(resetPeriod = "July through December"), date))
        assertEquals("2026", periodKeyFor(perk(resetPeriod = "Calendar year"), date))
    }

    @Test
    fun `reset month creates a month key and month end expiry`() {
        val perk = perk(resetPeriod = "December")

        assertEquals("2026-12", periodKeyFor(perk, date))
        assertEquals("2026-12", periodLabelFor(perk, date))
    }

    @Test
    fun `expiry status handles interval boundaries and deadline formats`() {
        assertTrue(isExpired(perk(deadline = "September 10"), LocalDate.of(2026, 9, 11)))
        assertFalse(isExpired(perk(interval = "Monthly"), LocalDate.of(2026, 9, 30)))
        assertTrue(isExpiringSoon(perk(interval = "Monthly"), LocalDate.of(2026, 9, 25)))
        assertTrue(isExpiringSoon(perk(deadline = "September 10"), date))
        assertFalse(isExpiringSoon(perk(deadline = "December 10"), date))
    }

    @Test
    fun `formats intervals and card filters for blank input`() {
        assertEquals("Monthly", prettyInterval(""))
        assertEquals("Quarterly", prettyInterval("quarterly"))
        assertEquals("No card", cardLabelForFilter("  "))
        assertEquals("Visa", cardLabelForFilter(" Visa "))
    }

    private fun perk(
        interval: String = "Monthly",
        resetPeriod: String = "",
        deadline: String = ""
    ) = PerkEntity(
        id = "id",
        title = "title",
        card = "card",
        interval = interval,
        sourceRowNumber = 1,
        resetPeriod = resetPeriod,
        deadlineTrigger = deadline,
        maxValueOrUses = "",
        details = "",
        usedFromSheet = false,
        usedAmountFromSheet = null
    )
}
