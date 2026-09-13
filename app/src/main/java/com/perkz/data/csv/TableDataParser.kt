package com.perkz.data.csv

import com.perkz.domain.BenefitUnit
import java.util.Locale
import java.util.UUID

enum class DraftSyncState {
    PENDING,
    ADDING,
    ADDED,
    ERROR
}

data class PerkDraft(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val card: String = "",
    val interval: String = "Monthly",
    val maxValue: String = "",
    val units: String = "USD",
    val resetPeriod: String = "",
    val deadline: String = "",
    val details: String = "",
    val syncState: DraftSyncState = DraftSyncState.PENDING,
    val errorMessage: String? = null
) {
    val isValid: Boolean
        get() = title.isNotBlank() && card.isNotBlank() && maxValue.isNotBlank()
}

object TableDataParser {

    fun parse(rawInput: String, defaultCard: String = ""): List<PerkDraft> {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return emptyList()

        val lines = trimmed.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val isMarkdown = lines.any { it.trim().startsWith("|") && it.trim().endsWith("|") }

        val rows: List<List<String>> = if (isMarkdown) {
            parseMarkdownTable(lines)
        } else {
            val sampleLine = lines.first()
            when {
                sampleLine.contains('\t') -> parseTsv(lines)
                sampleLine.contains(';') && !sampleLine.contains(',') -> parseCustomDelimiter(lines, ';')
                else -> parseCsvLines(trimmed)
            }
        }

        if (rows.isEmpty()) return emptyList()

        val rawHeaders = rows.first()
        val normalizedHeaders = rawHeaders.map { normalizeHeader(it) }

        val hasHeader = normalizedHeaders.any { h ->
            h in setOf(
                "benefit", "perk", "title", "name", "description",
                "card", "cardname", "cadence", "interval", "frequency",
                "resetperiod", "maxvalueuses", "maxvalue", "deadlinetrigger",
                "deadline", "notes", "details"
            )
        }

        val dataRows = if (hasHeader) rows.drop(1) else rows

        val titleIndex = if (hasHeader) {
            findHeaderIndex(normalizedHeaders, setOf("benefit", "perk", "title", "name", "description"))
                .takeIf { it >= 0 } ?: 0
        } else 0

        val cardIndex = if (hasHeader) {
            findHeaderIndex(normalizedHeaders, setOf("card", "cardname", "creditcard"))
        } else -1

        val intervalIndex = if (hasHeader) {
            findHeaderIndex(normalizedHeaders, setOf("cadence", "interval", "frequency"))
                .takeIf { it >= 0 } ?: 1
        } else 1

        val resetPeriodIndex = if (hasHeader) {
            findHeaderIndex(normalizedHeaders, setOf("resetperiod", "periodwindow", "reset"))
                .takeIf { it >= 0 } ?: 2
        } else 2

        val maxValueIndex = if (hasHeader) {
            findHeaderIndex(
                normalizedHeaders,
                setOf("maxvalueuses", "maxvalue", "maxuses", "value", "uses", "credit", "maxamount", "amount")
            ).takeIf { it >= 0 } ?: 3
        } else 3

        val deadlineIndex = if (hasHeader) {
            findHeaderIndex(normalizedHeaders, setOf("deadlinetrigger", "deadline", "trigger", "expiration"))
                .takeIf { it >= 0 } ?: 4
        } else 4

        val detailsIndex = if (hasHeader) {
            findHeaderIndex(normalizedHeaders, setOf("notes", "details", "description", "note"))
                .takeIf { it >= 0 } ?: 5
        } else 5

        val unitsIndex = if (hasHeader) {
            findHeaderIndex(normalizedHeaders, setOf("units", "unit", "currency"))
        } else -1

        return dataRows.mapNotNull { row ->
            val title = row.valueAt(titleIndex).trim()
            if (title.isBlank()) return@mapNotNull null

            val parsedCard = if (cardIndex >= 0) row.valueAt(cardIndex).trim() else ""
            val finalCard = parsedCard.ifBlank { defaultCard }

            val interval = row.valueAt(intervalIndex).trim().ifBlank { "Monthly" }
            val resetPeriod = row.valueAt(resetPeriodIndex).trim()
            val rawMaxValue = row.valueAt(maxValueIndex).trim()
            val deadline = row.valueAt(deadlineIndex).trim()
            val details = row.valueAt(detailsIndex).trim()

            val (extractedMaxValue, extractedUnits) = extractValueAndUnits(
                rawMaxValue = rawMaxValue,
                explicitUnits = if (unitsIndex >= 0) row.valueAt(unitsIndex) else ""
            )

            PerkDraft(
                title = title,
                card = finalCard,
                interval = interval,
                maxValue = extractedMaxValue,
                units = extractedUnits,
                resetPeriod = resetPeriod,
                deadline = deadline,
                details = details
            )
        }
    }

    private fun parseMarkdownTable(lines: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            // Skip markdown header separator row like |---|---|
            if (trimmed.matches(Regex("^\\|[\\s\\-:]+(\\|[\\s\\-:]+)+\\|$"))) continue

            val cells = trimmed.split('|')
                .map { it.trim() }
                .let { if (it.first().isEmpty()) it.drop(1) else it }
                .let { if (it.isNotEmpty() && it.last().isEmpty()) it.dropLast(1) else it }

            if (cells.isNotEmpty()) {
                rows.add(cells)
            }
        }
        return rows
    }

    private fun parseTsv(lines: List<String>): List<List<String>> {
        return lines.map { line ->
            line.split('\t').map { it.trim() }
        }
    }

    private fun parseCustomDelimiter(lines: List<String>, delimiter: Char): List<List<String>> {
        return lines.map { line ->
            line.split(delimiter).map { it.trim() }
        }
    }

    private fun parseCsvLines(input: String): List<List<String>> {
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
                    row.add(cell.toString().trim())
                    cell.clear()
                }
                (c == '\n' || c == '\r') && !inQuotes -> {
                    if (c == '\r' && i + 1 < input.length && input[i + 1] == '\n') {
                        i++
                    }
                    row.add(cell.toString().trim())
                    if (row.any { it.isNotBlank() }) {
                        rows.add(row.toList())
                    }
                    row.clear()
                    cell.clear()
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString().trim())
            if (row.any { it.isNotBlank() }) {
                rows.add(row.toList())
            }
        }
        return rows
    }

    private fun List<String>.valueAt(index: Int): String = if (index in indices) this[index] else ""

    private fun normalizeHeader(value: String): String {
        return value.lowercase(Locale.US).trim().replace(Regex("[^a-z0-9]"), "")
    }

    private fun extractValueAndUnits(rawMaxValue: String, explicitUnits: String): Pair<String, String> {
        val defaultUnit = if (explicitUnits.isNotBlank()) {
            BenefitUnit.fromSource(explicitUnits).code.uppercase()
        } else {
            "USD"
        }

        if (rawMaxValue.isBlank()) return Pair("", defaultUnit)

        var clean = rawMaxValue.trim()
        var unit = defaultUnit

        if (clean.contains("$") || clean.lowercase().contains("usd")) {
            unit = "USD"
            clean = clean.replace("$", "").replace("USD", "", ignoreCase = true).trim()
        }

        // Match regex like "Up to USD 595" or "USD 595" or "Up to 595"
        val numberMatch = Regex("([0-9]+(?:\\.[0-9]+)?)").find(clean)
        val extractedValue = numberMatch?.groupValues?.get(1) ?: clean

        return Pair(extractedValue, unit)
    }
}
