package com.perkz.domain

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
