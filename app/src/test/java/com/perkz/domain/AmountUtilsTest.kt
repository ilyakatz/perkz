package com.perkz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountUtilsTest {
    @Test
    fun `parses currency and decimal values`() {
        assertEquals(12.5, parseAmount("$12.50")!!, 0.0)
        assertEquals(12.5, parseAmount("€12,50")!!, 0.0)
        assertEquals(3.0, parseAmount("3 uses")!!, 0.0)
    }

    @Test
    fun `rejects missing and negative amounts`() {
        assertNull(parseAmount("unlimited"))
        assertNull(parseAmount("-5"))
    }

    @Test
    fun `formats whole and fractional amounts`() {
        assertEquals("12", formatAmount(12.0))
        assertEquals("12.5", formatAmount(12.5))
        assertEquals("12.35", formatAmount(12.345))
    }
}
