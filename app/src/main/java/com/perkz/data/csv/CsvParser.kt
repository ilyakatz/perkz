package com.perkz.data.csv

import com.perkz.data.db.PerkEntity
import com.perkz.domain.BenefitUnit
import java.security.MessageDigest
import java.util.Locale

data class ParsedCsv(
    val perks: List<PerkEntity>,
    val rawHeaders: List<String>
)

internal fun parsePerksFromCsv(csv: String): ParsedCsv {
    val rows = parseCsv(csv)
    if (rows.isEmpty()) return ParsedCsv(emptyList(), emptyList())

    val rawHeaders = rows.first()
    val header = rawHeaders.map { normalizeHeader(it) }
    val knownHeaders = setOf(
        "name", "title", "perk", "benefit", "card", "cardname",
        "interval", "frequency", "cadence", "resetperiod", "maxvalueuses",
        "deadlinetrigger", "notes", "details"
    )
    val hasHeader = header.any { it in knownHeaders }
    val dataRows = if (hasHeader) rows.drop(1) else rows

    val usedIndices = mutableSetOf<Int>()
    fun findAndMark(aliases: Set<String>, defaultIndex: Int): Int {
        val idx = findHeaderIndex(header, aliases, usedIndices)
        return if (idx >= 0) { usedIndices.add(idx); idx } else defaultIndex
    }

    val cardIndex = if (hasHeader) findAndMark(setOf("card", "cardname", "creditcard"), 0) else 0
    val titleIndex = if (hasHeader) findAndMark(setOf("perk", "benefit", "title", "name", "description", "perkname", "benefitname"), 1) else 1
    val intervalIndex = if (hasHeader) findAndMark(setOf("interval", "frequency", "cadence"), 2) else 2
    val resetPeriodIndex = if (hasHeader) findAndMark(setOf("resetperiod", "periodwindow"), 3) else 3
    val maxValueOrUsesIndex = if (hasHeader) findAndMark(setOf("maxvalue", "maxuses", "maxvalueuses", "value", "uses", "credit"), 4) else 4
    val deadlineIndex = if (hasHeader) findAndMark(setOf("deadlinetrigger", "deadline"), 5) else 5
    val detailsIndex = if (hasHeader) findAndMark(setOf("notes", "details", "description"), 6) else 6
    val unitsIndex = if (hasHeader) findHeaderIndex(header, setOf("units", "unit"), usedIndices).also { if (it >= 0) usedIndices.add(it) } else -1
    val usedIndex = if (hasHeader) findHeaderIndex(header, setOf("used"), usedIndices).also { if (it >= 0) usedIndices.add(it) } else -1
    val dateUsedIndex = if (hasHeader) findHeaderIndex(header, setOf("dateused"), usedIndices).also { if (it >= 0) usedIndices.add(it) } else -1

    val perks = dataRows.mapIndexedNotNull { index, row ->
        val title = row.valueAt(titleIndex).trim()
        if (title.isBlank()) return@mapIndexedNotNull null
        val card = row.valueAt(cardIndex).trim()
        val interval = row.valueAt(intervalIndex).trim().ifBlank { "Monthly" }
        val sourceRowNumber = index + if (hasHeader) 2 else 1
        val resetPeriod = row.valueAt(resetPeriodIndex).trim()
        val maxValueOrUses = row.valueAt(maxValueOrUsesIndex).trim()
        val deadlineTrigger = row.valueAt(deadlineIndex).trim()
        val details = row.valueAt(detailsIndex).trim()
        val benefitUnit = if (unitsIndex >= 0) {
            BenefitUnit.fromSource(row.valueAt(unitsIndex)).code
        } else {
            "auto"
        }
        val usedValue = if (usedIndex >= 0) row.valueAt(usedIndex) else ""
        val dateUsedValue = if (dateUsedIndex >= 0) row.valueAt(dateUsedIndex) else ""
        val usedAmountFromSheet = parseNumericUsedAmount(usedValue)
        val isNotApplicable = isMarkedNotApplicable(usedValue)
        val usedFromSheet = if (isNotApplicable) {
            false
        } else {
            usedAmountFromSheet?.let { it > 0.0 }
                ?: isMarkedUsedInSheet(usedValue, dateUsedValue)
        }
        val id = stableIdFrom("$title|$card|$interval|$resetPeriod|$deadlineTrigger|$maxValueOrUses|$details")
        PerkEntity(
            id = id,
            title = title,
            card = card,
            interval = interval,
            sourceRowNumber = sourceRowNumber,
            resetPeriod = resetPeriod,
            deadlineTrigger = deadlineTrigger,
            maxValueOrUses = maxValueOrUses,
            details = details,
            benefitUnit = benefitUnit,
            usedFromSheet = usedFromSheet,
            usedAmountFromSheet = usedAmountFromSheet,
            isNotApplicable = isNotApplicable
        )
    }
    return ParsedCsv(perks, rawHeaders)
}

private fun parseCsv(input: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val cell = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < input.length) {
        val c = input[i]
        when {
            c == '"' -> {
                if (inQuotes && i + 1 < input.length && input[i + 1] == '"') {
                    cell.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            }
            c == ',' && !inQuotes -> {
                row.add(cell.toString())
                cell.clear()
            }
            (c == '\n' || c == '\r') && !inQuotes -> {
                if (c == '\r' && i + 1 < input.length && input[i + 1] == '\n') {
                    i++
                }
                row.add(cell.toString())
                rows.add(row.toList())
                row.clear()
                cell.clear()
            }
            else -> cell.append(c)
        }
        i++
    }
    if (cell.isNotEmpty() || row.isNotEmpty()) {
        row.add(cell.toString())
        rows.add(row.toList())
    }
    return rows
}

private fun List<String>.valueAt(index: Int): String = if (index in indices) this[index] else ""

private fun normalizeHeader(value: String): String {
    return value.lowercase(Locale.US).trim().replace(Regex("[^a-z0-9]"), "")
}

internal fun findHeaderIndex(header: List<String>, aliases: Set<String>, excludeIndices: Set<Int> = emptySet()): Int {
    val exact = header.indices.firstOrNull { i -> i !in excludeIndices && header[i] in aliases }
    if (exact != null) return exact
    return header.indices.firstOrNull { i ->
        if (i in excludeIndices) return@firstOrNull false
        val h = header[i]
        aliases.any { alias ->
            if (alias == "name" && h.contains("card")) false
            else alias.length >= 4 && h.contains(alias)
        }
    } ?: -1
}

private fun stableIdFrom(value: String): String {
    val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray())
    return digest.joinToString("") { byte -> "%02x".format(byte) }
}

private fun isMarkedUsedInSheet(usedValue: String, dateUsedValue: String): Boolean {
    // Only mark as used if Date Used column has a value
    if (dateUsedValue.trim().isNotBlank()) return true

    // Only check the Used column if it has content
    val rawUsed = usedValue.trim()
    if (rawUsed.isBlank()) return false

    // For the Used column, check for explicit "yes", "true", "x", "✓" values
    val normalized = rawUsed.lowercase(Locale.US)
    return when {
        normalized == "yes" || normalized == "true" || normalized == "x" ||
        normalized == "✓" || normalized == "checked" -> true
        normalized == "no" || normalized == "false" || normalized == "" ||
        normalized == "n/a" || normalized == "na" -> false
        else -> false // Default to NOT used for any other value
    }

}

private fun isMarkedNotApplicable(usedValue: String): Boolean =
    usedValue.trim().equals("N/A", ignoreCase = true) ||
        usedValue.trim().equals("NA", ignoreCase = true)

private fun parseNumericUsedAmount(value: String): Double? {
    val raw = value.trim()
    if (raw.isBlank()) return null

    val normalized = raw
        .replace("$", "")
        .replace("€", "")
        .replace("£", "")
        .replace(" ", "")
    val number = when {
        normalized.contains(',') && normalized.contains('.') -> {
            if (normalized.lastIndexOf(',') > normalized.lastIndexOf('.')) {
                normalized.replace(".", "").replace(',', '.')
            } else {
                normalized.replace(",", "")
            }
        }
        normalized.contains(',') ->
            normalized.replace(',', '.')
        else -> normalized
    }.toDoubleOrNull()
    return number?.takeIf { it >= 0.0 }
}
