package com.perkz.domain

import com.perkz.domain.BenefitUnit
import java.util.Locale

internal fun parseAmount(value: String): Double? =
    Regex("""-?\d+(?:[.,]\d+)?""")
        .find(value)
        ?.value
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?.takeIf { it >= 0.0 }

internal fun formatAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString()
    else String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

/** True when the raw sheet value describes an integer-count unit (e.g. "3 uses") rather than currency. */
internal fun isCountUnit(rawMaxValueOrUses: String): Boolean {
    val normalized = rawMaxValueOrUses.lowercase(Locale.US)
    if (Regex("[$€£¥]").containsMatchIn(rawMaxValueOrUses)) return false
    return normalized.contains("use")
}

/** Currency symbol to prefix formatted amounts with, or blank for count-based units. */
internal fun currencyPrefixFor(rawMaxValueOrUses: String): String {
    Regex("[$€£¥]").find(rawMaxValueOrUses)?.let { return it.value }
    return if (isCountUnit(rawMaxValueOrUses)) "" else "$"
}

internal fun currencyPrefixFor(unit: BenefitUnit, rawMaxValueOrUses: String): String {
    return when (unit) {
        BenefitUnit.USD -> "$"
        BenefitUnit.NONE -> ""
        BenefitUnit.AUTO -> currencyPrefixFor(rawMaxValueOrUses)
        else -> ""
    }
}

/** Formats [value] using the unit implied by [rawMaxValueOrUses] (currency symbol or bare count). */
internal fun formatUnitAmount(rawMaxValueOrUses: String, value: Double): String =
    "${currencyPrefixFor(rawMaxValueOrUses)}${formatAmount(value)}"

internal fun formatUnitAmount(unit: BenefitUnit, rawMaxValueOrUses: String, value: Double): String {
    return when (unit) {
        BenefitUnit.USD -> "$${formatAmount(value)}"
        BenefitUnit.NONE -> formatAmount(value)
        BenefitUnit.AUTO -> formatUnitAmount(rawMaxValueOrUses, value)
        else -> formatAmount(value)
    }
}
