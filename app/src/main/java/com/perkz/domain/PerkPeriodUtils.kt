package com.perkz.domain

import com.perkz.data.db.PerkEntity
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val NO_CARD_FILTER = "No card"
private const val DEFAULT_EXPIRING_SOON_DAYS = 7L
private const val MONTHLY_EXPIRING_SOON_DAYS = 5L
private const val QUARTERLY_EXPIRING_SOON_DAYS = 21L

internal fun periodKeyFor(perk: PerkEntity, date: LocalDate): String {
    periodKeyFromResetPeriod(perk.resetPeriod, date)?.let { return it }
    return periodKeyForInterval(perk.interval, date)
}

private fun periodKeyForInterval(interval: String, date: LocalDate): String {
    val normalized = interval.lowercase(Locale.US).replace("-", "").replace(" ", "")
    return when {
        normalized.contains("month") -> date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        normalized.contains("quarter") -> "${date.year}-Q${((date.monthValue - 1) / 3) + 1}"
        normalized.contains("semiannual") || normalized.contains("biannual") || normalized.contains("6month") ->
            "${date.year}-H${if (date.monthValue <= 6) 1 else 2}"
        normalized.contains("year") || normalized.contains("annual") -> date.year.toString()
        else -> date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }
}

private fun periodKeyFromResetPeriod(resetPeriod: String, date: LocalDate): String? {
    val raw = resetPeriod.trim()
    if (raw.isBlank()) return null
    val normalized = raw.lowercase(Locale.US)

    monthFromText(normalized)?.let { month ->
        return "%04d-%02d".format(date.year, month)
    }
    if (normalized.contains("jan") && normalized.contains("jun")) return "${date.year}-H1"
    if (normalized.contains("jul") && normalized.contains("dec")) return "${date.year}-H2"
    if (normalized.contains("calendar year")) return date.year.toString()
    return null
}

internal fun periodLabelFor(perk: PerkEntity, date: LocalDate): String {
    return periodKeyFor(perk, date)
}

internal fun isExpiringSoon(perk: PerkEntity, date: LocalDate): Boolean {
    val expiry = expiryDateFor(perk, date) ?: return false
    val daysUntil = ChronoUnit.DAYS.between(date, expiry)
    return daysUntil in 0..expiringSoonThresholdDays(perk.interval)
}

private fun expiringSoonThresholdDays(interval: String): Long {
    val normalized = interval.lowercase(Locale.US).replace("-", "").replace(" ", "")
    return when {
        normalized.contains("month") -> MONTHLY_EXPIRING_SOON_DAYS
        normalized.contains("quarter") -> QUARTERLY_EXPIRING_SOON_DAYS
        else -> DEFAULT_EXPIRING_SOON_DAYS
    }
}

private fun expiryDateFor(perk: PerkEntity, date: LocalDate): LocalDate? {
    val resetExpiry = parseExpiryFromResetPeriod(perk.resetPeriod, date)
    parseExpiryFromDeadline(perk.deadlineTrigger, date, perk.resetPeriod)?.let { deadlineExpiry ->
        if (isGenericMonthEnd(perk.deadlineTrigger) && resetExpiry != null) return resetExpiry
        return deadlineExpiry
    }
    resetExpiry?.let { return it }
    return parseExpiryFromInterval(perk.interval, date)
}

private fun parseExpiryFromDeadline(deadline: String, date: LocalDate, resetPeriod: String): LocalDate? {
    val raw = deadline.trim()
    if (raw.isBlank()) return null
    val normalized = raw.lowercase(Locale.US)
    if (normalized.contains("month-end")) {
        val resetMonth = monthFromText(resetPeriod)
        if (resetMonth != null) {
            return YearMonth.of(date.year, resetMonth).atEndOfMonth()
        }
        return YearMonth.from(date).atEndOfMonth()
    }

    val month = monthFromText(normalized)
    val day = Regex("""\b([0-2]?\d|3[01])\b""").find(normalized)?.groupValues?.get(1)?.toIntOrNull()
    if (month != null && day != null) {
        return runCatching { LocalDate.of(date.year, month, day) }.getOrNull()
    }
    return null
}

private fun isGenericMonthEnd(deadline: String): Boolean {
    val normalized = deadline.trim().lowercase(Locale.US)
    return normalized.contains("month-end")
}

private fun parseExpiryFromResetPeriod(resetPeriod: String, date: LocalDate): LocalDate? {
    val raw = resetPeriod.trim()
    if (raw.isBlank()) return null
    val normalized = raw.lowercase(Locale.US)
    monthFromText(normalized)?.let { month ->
        return YearMonth.of(date.year, month).atEndOfMonth()
    }
    if (normalized.contains("jan") && normalized.contains("jun")) return LocalDate.of(date.year, Month.JUNE, 30)
    if (normalized.contains("jul") && normalized.contains("dec")) return LocalDate.of(date.year, Month.DECEMBER, 31)
    if (normalized.contains("calendar year")) return LocalDate.of(date.year, Month.DECEMBER, 31)
    return null
}

private fun parseExpiryFromInterval(interval: String, date: LocalDate): LocalDate? {
    val normalized = interval.lowercase(Locale.US).replace("-", "").replace(" ", "")
    return when {
        normalized.contains("month") -> YearMonth.from(date).atEndOfMonth()
        normalized.contains("quarter") -> {
            val endMonth = ((date.monthValue - 1) / 3 + 1) * 3
            YearMonth.of(date.year, endMonth).atEndOfMonth()
        }
        normalized.contains("semiannual") || normalized.contains("biannual") || normalized.contains("6month") ->
            if (date.monthValue <= 6) LocalDate.of(date.year, Month.JUNE, 30) else LocalDate.of(date.year, Month.DECEMBER, 31)
        normalized.contains("year") || normalized.contains("annual") -> LocalDate.of(date.year, Month.DECEMBER, 31)
        else -> null
    }
}

private fun monthFromText(value: String): Int? {
    val monthRegex = Regex(
        "\\b(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\b"
    )
    val match = monthRegex.find(value.lowercase(Locale.US))?.groupValues?.get(1) ?: return null
    return when {
        match.startsWith("jan") -> 1
        match.startsWith("feb") -> 2
        match.startsWith("mar") -> 3
        match.startsWith("apr") -> 4
        match == "may" -> 5
        match.startsWith("jun") -> 6
        match.startsWith("jul") -> 7
        match.startsWith("aug") -> 8
        match.startsWith("sep") -> 9
        match.startsWith("oct") -> 10
        match.startsWith("nov") -> 11
        match.startsWith("dec") -> 12
        else -> null
    }
}

internal fun prettyInterval(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return "Monthly"
    return value.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
}

internal fun cardLabelForFilter(rawCard: String): String {
    val trimmed = rawCard.trim()
    return if (trimmed.isBlank()) NO_CARD_FILTER else trimmed
}
