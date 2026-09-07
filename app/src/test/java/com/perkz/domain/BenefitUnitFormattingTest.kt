package com.perkz.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BenefitUnitFormattingTest {
    @Test
    fun `formats amount based on explicit benefit unit`() {
        // USD unit always gets a dollar sign
        assertEquals("$25", formatUnitAmount(BenefitUnit.USD, "", 25.0))
        assertEquals("$25", formatUnitAmount(BenefitUnit.USD, "100", 25.0))

        // NONE unit never gets a dollar sign, regardless of max value text
        assertEquals("25", formatUnitAmount(BenefitUnit.NONE, "$100", 25.0))
        assertEquals("25", formatUnitAmount(BenefitUnit.NONE, "100", 25.0))

        // AUTO unit follows existing heuristic (dollar sign if no "use" text)
        assertEquals("$25", formatUnitAmount(BenefitUnit.AUTO, "100", 25.0))
        assertEquals("25", formatUnitAmount(BenefitUnit.AUTO, "100 uses", 25.0))
        
        // Quantity units just format the bare number (labels handled in UI)
        assertEquals("5", formatUnitAmount(BenefitUnit.PASSES, "10", 5.0))
        assertEquals("500", formatUnitAmount(BenefitUnit.POINTS, "1000", 500.0))
    }

    @Test
    fun `resolves currency prefix based on explicit benefit unit`() {
        assertEquals("$", currencyPrefixFor(BenefitUnit.USD, "100"))
        assertEquals("", currencyPrefixFor(BenefitUnit.NONE, "$100"))
        assertEquals("$", currencyPrefixFor(BenefitUnit.AUTO, "100"))
        assertEquals("", currencyPrefixFor(BenefitUnit.AUTO, "100 uses"))
        assertEquals("", currencyPrefixFor(BenefitUnit.PASSES, "10"))
    }

    @Test
    fun `parses benefit unit from source text correctly`() {
        assertEquals(BenefitUnit.USD, BenefitUnit.fromSource("USD"))
        assertEquals(BenefitUnit.USD, BenefitUnit.fromSource("  $  "))
        assertEquals(BenefitUnit.PASSES, BenefitUnit.fromSource("Passes"))
        assertEquals(BenefitUnit.USES, BenefitUnit.fromSource("Use"))
        assertEquals(BenefitUnit.NONE, BenefitUnit.fromSource(""))
        assertEquals(BenefitUnit.NONE, BenefitUnit.fromSource("unknown"))
    }
}
