package com.perkz.data.csv

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvParserTest {
    @Test
    fun `reads numeric used amount from named sheet column`() {
        val perk = parsePerksFromCsv(
            "Name,Card,Interval,Max Value / Uses,Used,Date Used\n" +
                "Dining credit,Visa,Monthly,100,25,\n"
        ).single()

        assertEquals(25.0, perk.usedAmountFromSheet)
        assertEquals(true, perk.usedFromSheet)
    }

    @Test
    fun `uses fixed columns when sheet has no recognized header`() {
        val perk = parsePerksFromCsv("Visa,Dining credit,Monthly,,,\n").single()

        assertEquals("Dining credit", perk.title)
        assertEquals("Visa", perk.card)
        assertEquals("Monthly", perk.interval)
    }

    @Test
    fun `date used marks a sheet row used when amount is absent`() {
        val perk = parsePerksFromCsv(
            "Name,Card,Interval,Max Value / Uses,Used,Date Used\n" +
                "Dining credit,Visa,Monthly,100,,2026-09-01\n"
        ).single()

        assertEquals(null, perk.usedAmountFromSheet)
        assertEquals(true, perk.usedFromSheet)
    }

    @Test
    fun `supports quoted commas newlines and escaped quotes`() {
        val perk = parsePerksFromCsv(
            "Card,Name,Interval,Max Value,Notes\n" +
                "Visa,\"Dining, \"\"special\"\"\",Monthly,50,\"Line one\nLine two\"\n"
        ).single()

        assertEquals("Dining, \"special\"", perk.title)
        assertEquals("Line one\nLine two", perk.details)
        assertEquals(2, perk.sourceRowNumber)
    }

    @Test
    fun `recognizes explicit used markers without treating arbitrary text as used`() {
        val csv = "Name,Card,Used\n" +
            "Yes perk,Visa,yes\r\n" +
            "Checked perk,Visa,✓\r\n" +
            "No perk,Visa,no\r\n" +
            "Text perk,Visa,maybe\r\n"

        val perks = parsePerksFromCsv(csv)

        assertEquals(listOf(true, true, false, false), perks.map { it.usedFromSheet })
    }

    @Test
    fun `skips blank titles and parses localized used amounts`() {
        val perks = parsePerksFromCsv(
            "Name,Card,Used\n" +
                ",Visa,10\n" +
                "Travel,Visa,\"€1.234,50\"\n" +
                "Free,Visa,-2\n"
        )

        assertEquals(2, perks.size)
        assertEquals("Travel", perks[0].title)
        assertEquals(1234.50, perks[0].usedAmountFromSheet)
        assertEquals(null, perks[1].usedAmountFromSheet)
        assertEquals(false, perks[1].usedFromSheet)
    }
}
