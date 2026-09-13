package com.perkz.data.csv

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvParserTest {
    @Test
    fun `reads numeric used amount from named sheet column`() {
        val parsed = parsePerksFromCsv(
            "Name,Card,Interval,Max Value / Uses,Used,Date Used\n" +
                "Dining credit,Visa,Monthly,100,25,\n"
        )
        val perk = parsed.perks.single()

        assertEquals(25.0, perk.usedAmountFromSheet)
        assertEquals(true, perk.usedFromSheet)
        assertEquals(listOf("Name", "Card", "Interval", "Max Value / Uses", "Used", "Date Used"), parsed.rawHeaders)
    }

    @Test
    fun `uses fixed columns when sheet has no recognized header`() {
        val parsed = parsePerksFromCsv("Visa,Dining credit,Monthly,,,\n")
        val perk = parsed.perks.single()

        assertEquals("Dining credit", perk.title)
        assertEquals("Visa", perk.card)
        assertEquals("Monthly", perk.interval)
    }

    @Test
    fun `date used marks a sheet row used when amount is absent`() {
        val parsed = parsePerksFromCsv(
            "Name,Card,Interval,Max Value / Uses,Used,Date Used\n" +
                "Dining credit,Visa,Monthly,100,,2026-09-01\n"
        )
        val perk = parsed.perks.single()

        assertEquals(null, perk.usedAmountFromSheet)
        assertEquals(true, perk.usedFromSheet)
    }

    @Test
    fun `supports quoted commas newlines and escaped quotes`() {
        val parsed = parsePerksFromCsv(
            "Card,Name,Interval,Max Value,Notes\n" +
                "Visa,\"Dining, \"\"special\"\"\",Monthly,50,\"Line one\nLine two\"\n"
        )
        val perk = parsed.perks.single()

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

        val parsed = parsePerksFromCsv(csv)

        assertEquals(listOf(true, true, false, false), parsed.perks.map { it.usedFromSheet })
    }

    @Test
    fun `skips blank titles and parses localized used amounts`() {
        val parsed = parsePerksFromCsv(
            "Name,Card,Used\n" +
                ",Visa,10\n" +
                "Travel,Visa,\"€1.234,50\"\n" +
                "Free,Visa,-2\n"
        )

        assertEquals(2, parsed.perks.size)
        assertEquals("Travel", parsed.perks[0].title)
        assertEquals(1234.50, parsed.perks[0].usedAmountFromSheet)
        assertEquals(null, parsed.perks[1].usedAmountFromSheet)
        assertEquals(false, parsed.perks[1].usedFromSheet)
    }

    @Test
    fun `findHeaderIndex respects excludeIndices`() {
        val headers = listOf("cardname", "perkname", "cadence", "cadence")
        val normalized = headers.map { it.lowercase() }
        
        val idx1 = findHeaderIndex(normalized, setOf("cadence"))
        assertEquals(2, idx1)
        
        val idx2 = findHeaderIndex(normalized, setOf("cadence"), excludeIndices = setOf(2))
        assertEquals(3, idx2)
    }
}
