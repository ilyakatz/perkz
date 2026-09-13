package com.perkz.data.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TableDataParserTest {

    @Test
    fun `parse TSV table with standard headers`() {
        val tsvData = """
            Benefit	Cadence	Reset period	Max value / uses	Deadline / trigger	Notes
            Citi Annual Credit	Annual	Cardmembership year	Up to USD 595	Assessment date	Citigold tier
            Dining Credit	Monthly	End of month	$10	Last day of month	Resy credit
        """.trimIndent()

        val drafts = TableDataParser.parse(tsvData, defaultCard = "Citi Card")

        assertEquals(2, drafts.size)

        val first = drafts[0]
        assertEquals("Citi Annual Credit", first.title)
        assertEquals("Citi Card", first.card)
        assertEquals("Annual", first.interval)
        assertEquals("Cardmembership year", first.resetPeriod)
        assertEquals("595", first.maxValue)
        assertEquals("USD", first.units)
        assertEquals("Assessment date", first.deadline)
        assertEquals("Citigold tier", first.details)
        assertTrue(first.isValid)

        val second = drafts[1]
        assertEquals("Dining Credit", second.title)
        assertEquals("Monthly", second.interval)
        assertEquals("10", second.maxValue)
    }

    @Test
    fun `parse CSV table with card column`() {
        val csvData = """
            Title,Card,Interval,Max Value,Reset Period,Deadline
            Uber Cash,Amex Gold,Monthly,10,End of month,Last day
            Flight Credit,Chase Sapphire,Annual,300,Calendar year,Dec 31
        """.trimIndent()

        val drafts = TableDataParser.parse(csvData)

        assertEquals(2, drafts.size)
        assertEquals("Amex Gold", drafts[0].card)
        assertEquals("Chase Sapphire", drafts[1].card)
    }

    @Test
    fun `parse Markdown table format`() {
        val markdownData = """
            | Benefit | Cadence | Reset period | Max value / uses | Deadline / trigger |
            |---|---|---|---|---|
            | Streaming Credit | Monthly | End of month | 20 | Last day |
            | Hotel Credit | Annual | Calendar year | 200 | Dec 31 |
        """.trimIndent()

        val drafts = TableDataParser.parse(markdownData, defaultCard = "Amex Platinum")

        assertEquals(2, drafts.size)
        assertEquals("Streaming Credit", drafts[0].title)
        assertEquals("Amex Platinum", drafts[0].card)
        assertEquals("Monthly", drafts[0].interval)
        assertEquals("20", drafts[0].maxValue)
    }

    @Test
    fun `draft validation logic`() {
        val valid = PerkDraft(title = "Valid Title", card = "Valid Card", maxValue = "100")
        val invalidNoTitle = PerkDraft(title = "", card = "Card", maxValue = "100")
        val invalidNoCard = PerkDraft(title = "Title", card = "", maxValue = "100")
        val invalidNoMax = PerkDraft(title = "Title", card = "Card", maxValue = "")

        assertTrue(valid.isValid)
        assertFalse(invalidNoTitle.isValid)
        assertFalse(invalidNoCard.isValid)
        assertFalse(invalidNoMax.isValid)
    }
}
