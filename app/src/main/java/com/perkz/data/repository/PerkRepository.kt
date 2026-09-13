package com.perkz.data.repository

import android.util.Log
import com.perkz.data.csv.parsePerksFromCsv
import com.perkz.data.db.PerkDao
import com.perkz.data.db.PerkEntity
import com.perkz.data.db.SyncStatusEntity
import com.perkz.data.db.UsageEntity
import com.perkz.domain.periodKeyFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DATE_USED_FORMAT = "MMM d"

class PerkRepository(private val dao: PerkDao) {

    val allPerks: Flow<List<PerkEntity>> = dao.observePerks()

    val allUsage: Flow<List<UsageEntity>> = dao.observeUsage()

    fun observePerks(): Flow<List<PerkEntity>> = dao.observePerks()

    fun observeUsage(): Flow<List<UsageEntity>> = dao.observeUsage()

    fun observeSyncStatus(): Flow<SyncStatusEntity?> = dao.observeSyncStatus()

    suspend fun currentUsageAmount(perk: PerkEntity): Double {
        val periodKey = periodKeyFor(perk, LocalDate.now())
        return dao.getUsage(perk.id, periodKey)?.amount
            ?: perk.usedAmountFromSheet
            ?: if (perk.usedFromSheet) com.perkz.domain.parseAmount(perk.maxValueOrUses) ?: 1.0 else 0.0
    }

    suspend fun refresh(sheetUrl: String) {
        // Add cache-busting query parameter to bypass Google's CDN cache
        val cacheBustUrl = if (sheetUrl.contains("?")) {
            "$sheetUrl&cache=${System.currentTimeMillis()}"
        } else {
            "$sheetUrl?cache=${System.currentTimeMillis()}"
        }
        try {
            Log.d("PerkRepository", "Fetching CSV from: $cacheBustUrl")
            val csv = withContext(Dispatchers.IO) { URL(cacheBustUrl).readText() }
            Log.d("PerkRepository", "CSV fetched, size: ${csv.length} bytes")
            if (csv.isBlank()) {
                throw IllegalStateException("CSV returned empty - sheet may not be shared publicly")
            }
            val parsed = parsePerksFromCsv(csv)
            val parsedPerks = parsed.perks
            Log.d("PerkRepository", "Parsed ${parsedPerks.size} perks from CSV")
            if (parsedPerks.isEmpty()) {
                throw IllegalStateException("No perks parsed from CSV - check sheet format")
            }
            // Only clear and insert if we successfully parsed perks
            dao.clearPerks()
            // The sheet is the source of truth when refreshed, including cleared Used values.
            dao.clearUsage()
            dao.insertPerks(parsedPerks)
            dao.upsertSyncStatus(
                SyncStatusEntity(
                    lastSyncedAtEpochMillis = System.currentTimeMillis(),
                    rawHeadersJson = parsed.rawHeaders.toJsonArray()
                )
            )
            Log.d("PerkRepository", "Successfully refreshed and stored ${parsedPerks.size} perks")
        } catch (e: Exception) {
            Log.e("PerkRepository", "Refresh failed: ${e.message}", e)
            throw IllegalStateException("Failed to refresh perks: ${e.message}", e)
        }
    }

    suspend fun setUsed(
        perk: PerkEntity,
        checked: Boolean,
        sheetUrl: String? = null,
        webhookUrl: String? = null
    ): ToggleSyncResult {
        val amount = if (checked) {
            com.perkz.domain.parseAmount(perk.maxValueOrUses) ?: 1.0
        } else {
            0.0
        }
        return setUsedAmount(perk, amount, sheetUrl, webhookUrl)
    }

    suspend fun setUsedAmount(
        perk: PerkEntity,
        amount: Double,
        sheetUrl: String? = null,
        webhookUrl: String? = null
    ): ToggleSyncResult {
        val periodKey = periodKeyFor(perk, LocalDate.now())
        if (amount > 0.0) {
            dao.upsertUsage(UsageEntity(perkId = perk.id, periodKey = periodKey, amount = amount))
        } else {
            dao.deleteUsage(perk.id, periodKey)
        }
        dao.updateUsedFromSheet(perk.id, amount > 0.0)

        val hasWebhook = !webhookUrl.isNullOrBlank()
        if (hasWebhook) {
            withContext(Dispatchers.IO) {
                updateSheetViaWebhook(
                    webhookUrl = webhookUrl!!,
                    sheetUrl = sheetUrl.orEmpty(),
                    rowNumber = perk.sourceRowNumber,
                    checked = amount > 0.0,
                    usedValue = if (amount > 0.0) amount.toString() else "",
                    dao = dao
                )
            }
        } else if (amount > 0.0 && sheetUrl != null) {
            throw IllegalStateException("Set 'Update webhook URL (Apps Script)' in Settings first.")
        }
        return if (hasWebhook) ToggleSyncResult.SyncedToSheet else ToggleSyncResult.LocalOnly
    }

    suspend fun updateLocalUsed(perk: PerkEntity, checked: Boolean) {
        val periodKey = periodKeyFor(perk, LocalDate.now())
        if (checked) {
            val amount = com.perkz.domain.parseAmount(perk.maxValueOrUses) ?: 1.0
            dao.upsertUsage(UsageEntity(perkId = perk.id, periodKey = periodKey, amount = amount))
        } else {
            dao.deleteUsage(perk.id, periodKey)
        }
        dao.updateUsedFromSheet(perk.id, checked)
    }

    suspend fun setNotApplicable(
        perk: PerkEntity,
        notApplicable: Boolean,
        sheetUrl: String? = null,
        webhookUrl: String? = null
    ): ToggleSyncResult {
        if (notApplicable) {
            dao.deleteUsage(perk.id, periodKeyFor(perk, LocalDate.now()))
        }
        dao.insertPerks(
            listOf(
                perk.copy(
                    usedFromSheet = false,
                    usedAmountFromSheet = null,
                    isNotApplicable = notApplicable
                )
            )
        )

        val hasWebhook = !webhookUrl.isNullOrBlank()
        if (hasWebhook) {
            withContext(Dispatchers.IO) {
                updateSheetViaWebhook(
                    webhookUrl = webhookUrl!!,
                    sheetUrl = sheetUrl.orEmpty(),
                    rowNumber = perk.sourceRowNumber,
                    checked = false,
                    usedValue = if (notApplicable) "N/A" else "",
                    dao = dao
                )
            }
        } else if (sheetUrl != null) {
            throw IllegalStateException("Set 'Update webhook URL (Apps Script)' in Settings first.")
        }
        return if (hasWebhook) ToggleSyncResult.SyncedToSheet else ToggleSyncResult.LocalOnly
    }

    suspend fun addPerk(
        title: String,
        card: String,
        interval: String,
        maxValue: String,
        units: String,
        resetPeriod: String,
        deadline: String,
        details: String,
        sheetUrl: String,
        webhookUrl: String
    ) {
        val syncStatus = dao.observeSyncStatus().first()
        val headers = syncStatus?.rawHeadersJson?.fromJsonArray() ?: emptyList()
        
        if (headers.isEmpty()) {
            throw IllegalStateException("App hasn't learned your sheet structure. Please tap 'Refresh' on the Perks tab once.")
        }

        val normalizedHeaders = headers.map { h -> h.lowercase(Locale.US).trim().replace(Regex("[^a-z0-9]"), "") }
        val indexValues = mutableMapOf<Int, String>()
        val usedIndices = mutableSetOf<Int>()
        
        fun mapField(aliases: Set<String>, value: String) {
            val idx = com.perkz.data.csv.findHeaderIndex(normalizedHeaders, aliases, usedIndices)
            if (idx != -1) {
                indexValues[idx] = value
                usedIndices.add(idx)
            }
        }

        mapField(setOf("card", "cardname"), card)
        mapField(setOf("perkname", "perk", "benefit", "title", "name", "description"), title)
        mapField(setOf("interval", "frequency", "cadence"), interval)
        mapField(setOf("resetperiod", "periodwindow", "period", "window", "cadence"), resetPeriod)
        mapField(setOf("maxvalue", "maxuses", "maxvalueuses", "value", "uses", "credit"), maxValue)
        mapField(setOf("deadlinetrigger", "deadline", "trigger"), deadline)
        mapField(setOf("notes", "details", "description"), details)
        mapField(setOf("units", "unit"), units)

        if (indexValues.isEmpty()) {
            throw IllegalStateException("Could not match your app fields to any columns in your sheet. Check your headers.")
        }

        withContext(Dispatchers.IO) {
            val payload = JSONObject().apply {
                put("action", "append")
                put("sheetId", parseSheetId(sheetUrl))
                put("gid", parseGid(sheetUrl))
                put("updates", JSONObject().apply {
                    indexValues.forEach { (k, v) -> put(k.toString(), v) }
                })
            }

            Log.d("PerkRepository", "Sending Append Payload: $payload")
            postToWebhook(webhookUrl, payload.toString())
        }
        refresh(sheetUrl)
    }

    suspend fun addPerksBulk(
        perks: List<com.perkz.data.csv.PerkDraft>,
        sheetUrl: String,
        webhookUrl: String,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ) {
        val syncStatus = dao.observeSyncStatus().first()
        val headers = syncStatus?.rawHeadersJson?.fromJsonArray() ?: emptyList()
        
        if (headers.isEmpty()) {
            throw IllegalStateException("App hasn't learned your sheet structure. Please tap 'Refresh' on the Perks tab once.")
        }

        val validPerks = perks.filter { it.isValid }
        if (validPerks.isEmpty()) {
            throw IllegalStateException("No valid perks to add.")
        }

        val normalizedHeaders = headers.map { h -> h.lowercase(Locale.US).trim().replace(Regex("[^a-z0-9]"), "") }

        withContext(Dispatchers.IO) {
            validPerks.forEachIndexed { index, perk ->
                onProgress(index + 1, validPerks.size)
                val indexValues = mutableMapOf<Int, String>()
                val usedIndices = mutableSetOf<Int>()

                fun mapField(aliases: Set<String>, value: String) {
                    val idx = com.perkz.data.csv.findHeaderIndex(normalizedHeaders, aliases, usedIndices)
                    if (idx != -1) {
                        indexValues[idx] = value
                        usedIndices.add(idx)
                    }
                }

                mapField(setOf("card", "cardname"), perk.card)
                mapField(setOf("perkname", "perk", "benefit", "title", "name", "description"), perk.title)
                mapField(setOf("interval", "frequency", "cadence"), perk.interval)
                mapField(setOf("resetperiod", "periodwindow", "period", "window", "cadence"), perk.resetPeriod)
                mapField(setOf("maxvalue", "maxuses", "maxvalueuses", "value", "uses", "credit"), perk.maxValue)
                mapField(setOf("deadlinetrigger", "deadline", "trigger"), perk.deadline)
                mapField(setOf("notes", "details", "description"), perk.details)
                mapField(setOf("units", "unit"), perk.units)

                if (indexValues.isNotEmpty()) {
                    val payload = JSONObject().apply {
                        put("action", "append")
                        put("sheetId", parseSheetId(sheetUrl))
                        put("gid", parseGid(sheetUrl))
                        put("updates", JSONObject().apply {
                            indexValues.forEach { (k, v) -> put(k.toString(), v) }
                        })
                    }

                    Log.d("PerkRepository", "Sending Bulk Append Payload (${index + 1}/${validPerks.size}): $payload")
                    postToWebhook(webhookUrl, payload.toString())
                }
            }
        }
        refresh(sheetUrl)
    }

    suspend fun addSingleDraft(
        draft: com.perkz.data.csv.PerkDraft,
        sheetUrl: String,
        webhookUrl: String
    ) {
        val syncStatus = dao.observeSyncStatus().first()
        val headers = syncStatus?.rawHeadersJson?.fromJsonArray() ?: emptyList()
        
        if (headers.isEmpty()) {
            throw IllegalStateException("App hasn't learned your sheet structure. Please tap 'Refresh' on the Perks tab once.")
        }

        val normalizedHeaders = headers.map { h -> h.lowercase(Locale.US).trim().replace(Regex("[^a-z0-9]"), "") }
        val indexValues = mutableMapOf<Int, String>()
        val usedIndices = mutableSetOf<Int>()

        fun mapField(aliases: Set<String>, value: String) {
            val idx = com.perkz.data.csv.findHeaderIndex(normalizedHeaders, aliases, usedIndices)
            if (idx != -1) {
                indexValues[idx] = value
                usedIndices.add(idx)
            }
        }

        mapField(setOf("card", "cardname"), draft.card)
        mapField(setOf("perkname", "perk", "benefit", "title", "name", "description"), draft.title)
        mapField(setOf("interval", "frequency", "cadence"), draft.interval)
        mapField(setOf("resetperiod", "periodwindow", "period", "window", "cadence"), draft.resetPeriod)
        mapField(setOf("maxvalue", "maxuses", "maxvalueuses", "value", "uses", "credit"), draft.maxValue)
        mapField(setOf("deadlinetrigger", "deadline", "trigger"), draft.deadline)
        mapField(setOf("notes", "details", "description"), draft.details)
        mapField(setOf("units", "unit"), draft.units)

        if (indexValues.isEmpty()) {
            throw IllegalStateException("Could not match your app fields to any columns in your sheet.")
        }

        withContext(Dispatchers.IO) {
            val payload = JSONObject().apply {
                put("action", "append")
                put("sheetId", parseSheetId(sheetUrl))
                put("gid", parseGid(sheetUrl))
                put("updates", JSONObject().apply {
                    indexValues.forEach { (k, v) -> put(k.toString(), v) }
                })
            }

            Log.d("PerkRepository", "Sending Single Append Payload: $payload")
            postToWebhook(webhookUrl, payload.toString())
        }
    }
}

private fun List<String>.toJsonArray(): String = JSONArray(this).toString()

private fun String.fromJsonArray(): List<String> {
    if (this.isBlank() || this == "[]") return emptyList()
    return try {
        val arr = JSONArray(this)
        List(arr.length()) { i -> arr.getString(i) }
    } catch (e: Exception) {
        emptyList()
    }
}

enum class ToggleSyncResult {
    SyncedToSheet,
    LocalOnly
}

private suspend fun updateSheetViaWebhook(
    webhookUrl: String,
    sheetUrl: String,
    rowNumber: Int,
    checked: Boolean,
    usedValue: String,
    dao: PerkDao? = null
) {
    val sheetId = parseSheetId(sheetUrl)
    val gid = parseGid(sheetUrl)
    val dateUsed = if (checked) {
        LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_USED_FORMAT, Locale.US))
    } else {
        ""
    }

    val indexValues = mutableMapOf<Int, String>()
    if (dao != null) {
        val syncStatus = dao.observeSyncStatus().first()
        val headers = syncStatus?.rawHeadersJson?.fromJsonArray() ?: emptyList()
        val normalizedHeaders = headers.map { h -> h.lowercase(Locale.US).trim().replace(Regex("[^a-z0-9]"), "") }
        
        val dateUsedIdx = com.perkz.data.csv.findHeaderIndex(normalizedHeaders, setOf("dateused"))
        if (dateUsedIdx != -1) indexValues[dateUsedIdx] = dateUsed
        
        val usedIdx = com.perkz.data.csv.findHeaderIndex(normalizedHeaders, setOf("used"))
        if (usedIdx != -1) indexValues[usedIdx] = usedValue
    }

    val payload = JSONObject().apply {
        put("sheetId", sheetId)
        put("gid", gid)
        put("rowNumber", rowNumber)
        put("updates", JSONObject().apply {
            indexValues.forEach { (k, v) -> put(k.toString(), v) }
        })
    }

    postToWebhook(webhookUrl, payload.toString())
}

private fun parseSheetId(sheetUrl: String): String {
    return Regex("/d/([a-zA-Z0-9-_]+)")
        .find(sheetUrl)
        ?.groupValues
        ?.getOrNull(1)
        ?: throw IllegalArgumentException("Could not parse sheet ID from URL")
}

private fun parseGid(sheetUrl: String): String {
    return Regex("[?&]gid=([0-9]+)")
        .find(sheetUrl)
        ?.groupValues
        ?.getOrNull(1)
        ?: "0"
}

private fun postToWebhook(webhookUrl: String, body: String) {
    try {
        val connection = (URL(webhookUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = true
        }
        connection.outputStream.use { stream ->
            stream.write(body.toByteArray(Charsets.UTF_8))
        }
        val code = connection.responseCode
        if (code !in 200..299) {
            val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
            throw IllegalStateException("Webhook update failed ($code): $errorText")
        }
        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        if (responseText.isNotBlank() && !Regex("\"ok\"\\s*:\\s*true").containsMatchIn(responseText)) {
            throw IllegalStateException("Webhook did not confirm success: $responseText")
        }
    } catch (e: Exception) {
        throw IllegalStateException("Webhook error: ${e.message}", e)
    }
}

private fun jsonEscape(input: String): String {
    return input
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
