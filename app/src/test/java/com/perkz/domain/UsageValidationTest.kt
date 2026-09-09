package com.perkz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageValidationTest {

    @Test
    fun `regular usage - validation and limits`() {
        // $100 max, $25 used -> $75 remaining
        val result = validateUsage(
            amountText = "50",
            usedAmount = 25.0,
            maxAmount = 100.0,
            isSubtraction = false
        )
        assertEquals(75.0, result.limit!!, 0.0)
        assertFalse(result.exceedsLimit)
        assertTrue(result.isValid)
    }

    @Test
    fun `regular usage - exceeds limit`() {
        val result = validateUsage(
            amountText = "80",
            usedAmount = 25.0,
            maxAmount = 100.0,
            isSubtraction = false
        )
        assertTrue(result.exceedsLimit)
        assertFalse(result.isValid)
    }

    @Test
    fun `subtraction - validation and limits`() {
        // $25 used -> can subtract up to $25
        val result = validateUsage(
            amountText = "10",
            usedAmount = 25.0,
            maxAmount = 100.0,
            isSubtraction = true
        )
        assertEquals(25.0, result.limit!!, 0.0)
        assertFalse(result.exceedsLimit)
        assertTrue(result.isValid)
    }

    @Test
    fun `subtraction - exceeds used amount`() {
        val result = validateUsage(
            amountText = "30",
            usedAmount = 25.0,
            maxAmount = 100.0,
            isSubtraction = true
        )
        assertTrue(result.exceedsLimit)
        assertFalse(result.isValid)
    }

    @Test
    fun `invalid inputs are not valid`() {
        val result = validateUsage(
            amountText = "abc",
            usedAmount = 25.0,
            maxAmount = 100.0,
            isSubtraction = false
        )
        assertFalse(result.isValid)
    }

    @Test
    fun `zero or negative inputs are not valid`() {
        assertFalse(validateUsage("0", 25.0, 100.0, false).isValid)
        assertFalse(validateUsage("-5", 25.0, 100.0, false).isValid)
    }
}
