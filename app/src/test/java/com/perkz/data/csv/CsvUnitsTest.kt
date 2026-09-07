package com.perkz.data.csv

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvUnitsTest {
    @Test
    fun `parses unit column into benefit unit`() {
        val csv = "Name,Card,Interval,Max Value,Used,Units\n" +
            "USD Perk,Visa,Monthly,100,0,usd\n" +
            "Uses Perk,Visa,Monthly,100,0,uses\n" +
            "Passes Perk,Visa,Monthly,1,0,pass\n" +
            "Points Perk,Visa,Monthly,100,0,points\n" +
            "Miles Perk,Visa,Monthly,100,0,miles\n" +
            "Nights Perk,Visa,Monthly,5,0,nights\n" +
            "Months Perk,Visa,Monthly,12,0,months\n" +
            "Guests Perk,Visa,Monthly,1,0,guest\n" +
            "Access Perk,Visa,Monthly,1,0,access\n" +
            "None Perk,Visa,Monthly,100,0,none\n" +
            "Empty Perk,Visa,Monthly,100,0,\n" +
            "Unknown Perk,Visa,Monthly,100,0,waffles\n"

        val perks = parsePerksFromCsv(csv)

        assertEquals("usd", perks[0].benefitUnit)
        assertEquals("uses", perks[1].benefitUnit)
        assertEquals("passes", perks[2].benefitUnit)
        assertEquals("points", perks[3].benefitUnit)
        assertEquals("miles", perks[4].benefitUnit)
        assertEquals("nights", perks[5].benefitUnit)
        assertEquals("months", perks[6].benefitUnit)
        assertEquals("guests", perks[7].benefitUnit)
        assertEquals("access", perks[8].benefitUnit)
        assertEquals("none", perks[9].benefitUnit)
        assertEquals("none", perks[10].benefitUnit)
        assertEquals("none", perks[11].benefitUnit)
    }

    @Test
    fun `falls back to auto when unit column is missing`() {
        val csv = "Name,Card,Interval,Max Value,Used\n" +
            "Auto Perk,Visa,Monthly,100,0\n"

        val perk = parsePerksFromCsv(csv).single()

        assertEquals("auto", perk.benefitUnit)
    }
}
