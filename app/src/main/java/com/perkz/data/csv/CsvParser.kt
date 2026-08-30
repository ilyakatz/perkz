package com.perkz.data.csv

import com.perkz.data.db.PerkEntity
import java.security.MessageDigest
import java.util.Locale

internal fun parsePerksFromCsv(csv: String): List<PerkEntity> {
    val rows = parseCsv(csv)
    if (rows.isEmpty()) return emptyList()

    val header = rows.first().map { normalizeHeader(it) }
    val hasHeader = header.any {
        it in setOf(
            "name",
            "title",
            "perk",
            "benefit",
            "card",
            "cardname",
            "interval",
            "frequency",
            "cadence",
            "resetperiod",
            "maxvalueuses",
            "deadlinetrigger",
            "notes",
            "details"
        )
    }

    val dataRows = if (hasHeader) rows.drop(1) else rows

    // For sheets with headers, use header-based matching
    // For sheets without, use fixed column positions
    val titleIndex = if (hasHeader) {
        findHeaderIndex(header, setOf("perk", "benefit", "title", "name", "description"))
            .takeIf { it >= 0 } ?: 1
    } else {
        1
    }
    val cardIndex = if (hasHeader) {
        findHeaderIndex(header, setOf("card", "cardname")).takeIf { it >= 0 } ?: 0
    } else {
        0
    }
    val intervalIndex = if (hasHeader) {
        findHeaderIndex(header, setOf("interval", "frequency", "cadence"))
            .takeIf { it >= 0 } ?: 2
    } else {
        2
    }
    val resetPeriodIndex = if (hasHeader) {
        findHeaderIndex(header, setOf("resetperiod", "periodwindow"))
            .takeIf { it >= 0 } ?: 3
    } else {
        3
    }
    val maxValueOrUsesIndex = findHeaderIndex(
        header,
        setOf("maxvalue", "maxuses", "maxvalueuses", "value", "uses", "credit")
    ).takeIf { it >= 0 } ?: 4
    val deadlineIndex = findHeaderIndex(header, setOf("deadlinetrigger", "deadline"))
        .takeIf { it >= 0 } ?: 5
    val detailsIndex = findHeaderIndex(header, setOf("notes", "details", "description"))
        .takeIf { it >= 0 } ?: 6
    val usedIndex = findHeaderIndex(header, setOf("used"))
    val dateUsedIndex = findHeaderIndex(header, setOf("dateused"))

    return dataRows.mapIndexedNotNull { index, row ->
        val title = row.valueAt(titleIndex).trim()
        if (title.isBlank()) return@mapIndexedNotNull null
        val card = row.valueAt(cardIndex).trim()
        val interval = row.valueAt(intervalIndex).trim().ifBlank { "Monthly" }
        val sourceRowNumber = index + if (hasHeader) 2 else 1
        val resetPeriod = row.valueAt(resetPeriodIndex).trim()
        val maxValueOrUses = row.valueAt(maxValueOrUsesIndex).trim()
        val deadlineTrigger = row.valueAt(deadlineIndex).trim()
        val details = row.valueAt(detailsIndex).trim()
        val usedValue = if (usedIndex >= 0) row.valueAt(usedIndex) else ""
        val dateUsedValue = if (dateUsedIndex >= 0) row.valueAt(dateUsedIndex) else ""
        val usedFromSheet = isMarkedUsedInSheet(usedValue, dateUsedValue)
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
            usedFromSheet = usedFromSheet
        )
    }
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

private fun findHeaderIndex(header: List<String>, aliases: Set<String>): Int {
    val exact = header.indexOfFirst { it in aliases }
    if (exact >= 0) return exact
    return header.indexOfFirst { normalized ->
        aliases.any { alias -> alias.length >= 5 && normalized.contains(alias) }
    }
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
