package com.perkz.domain

import com.perkz.data.db.PerkEntity
import com.perkz.ui.model.PerkStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class NotApplicableStatusTest {
    @Test
    fun `not applicable status takes precedence over usage and expiry`() {
        val perk = PerkEntity(
            id = "global-entry",
            title = "Global entry",
            card = "Amex",
            interval = "Monthly",
            sourceRowNumber = 2,
            resetPeriod = "",
            deadlineTrigger = "September 1",
            maxValueOrUses = "100",
            details = "",
            benefitUnit = "auto",
            usedFromSheet = true,
            usedAmountFromSheet = 50.0,
            isNotApplicable = true
        )

        assertEquals(
            PerkStatus.NotApplicable,
            classifyPerkStatus(perk, LocalDate.of(2026, 9, 6), 50.0)
        )
    }
}
