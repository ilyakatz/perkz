package com.perkz.data.csv

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvNotApplicableTest {
    @Test
    fun `reads not applicable marker without treating it as numeric or used`() {
        val perk = parsePerksFromCsv(
            "Name,Card,Used,Date Used\n" +
                "Global entry,Amex,N/A,\n"
        ).single()

        assertEquals(true, perk.isNotApplicable)
        assertEquals(false, perk.usedFromSheet)
        assertEquals(null, perk.usedAmountFromSheet)
    }

    @Test
    fun `does not treat not applicable marker as used when date is present`() {
        val perk = parsePerksFromCsv(
            "Name,Card,Used,Date Used\n" +
                "Global entry,Amex,N/A,2026-09-01\n"
        ).single()

        assertEquals(true, perk.isNotApplicable)
        assertEquals(false, perk.usedFromSheet)
        assertEquals(null, perk.usedAmountFromSheet)
    }
}
