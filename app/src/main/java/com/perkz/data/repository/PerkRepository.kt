package com.perkz.data.repository

import android.util.Log
import com.perkz.data.csv.parsePerksFromCsv
import com.perkz.data.db.PerkDao
import com.perkz.data.db.PerkEntity
import com.perkz.data.db.UsageEntity
import com.perkz.domain.periodKeyFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DATE_USED_FORMAT = "MMM d"

class PerkRepository(private val dao: PerkDao) {

    fun observePerks(): Flow<List<PerkEntity>> = dao.observePerks()

    fun observeUsage(): Flow<List<UsageEntity>> = dao.observeUsage()

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
            val parsedPerks = parsePerksFromCsv(csv)
            Log.d("PerkRepository", "Parsed ${parsedPerks.size} perks from CSV")
            if (parsedPerks.isEmpty()) {
                throw IllegalStateException("No perks parsed from CSV - check sheet format")
            }
            // Only clear and insert if we successfully parsed perks
            dao.clearPerks()
            // The sheet is the source of truth after a successful refresh.
            dao.clearUsage()
            dao.insertPerks(parsedPerks)
            Log.d("PerkRepository", "Successfully refreshed and stored ${parsedPerks.size} perks")
        } catch (e: Exception) {
            Log.e("PerkRepository", "Refresh failed: ${e.message}", e)
            throw IllegalStateException("Failed to refresh perks: ${e.message}", e)
        }
    }

    suspend fun setUsed(
        perk: PerkEntity,
        checked: Boolean,
        sheetUrl: String,
        webhookUrl: String
    ): ToggleSyncResult {
        updateLocalUsed(perk, checked)

        val hasWebhook = webhookUrl.isNotBlank()
        if (hasWebhook) {
            withContext(Dispatchers.IO) {
                updateSheetViaWebhook(
                    webhookUrl = webhookUrl,
                    sheetUrl = sheetUrl,
                    rowNumber = perk.sourceRowNumber,
                    checked = checked
                )
            }
        } else if (checked) {
            throw IllegalStateException("Set 'Update webhook URL (Apps Script)' in Settings first.")
        }

        return if (hasWebhook) ToggleSyncResult.SyncedToSheet else ToggleSyncResult.LocalOnly
    }

    suspend fun updateLocalUsed(perk: PerkEntity, checked: Boolean) {
        val periodKey = periodKeyFor(perk, LocalDate.now())
        if (checked) {
            dao.upsertUsage(UsageEntity(perkId = perk.id, periodKey = periodKey))
        } else {
            dao.deleteUsage(perk.id, periodKey)
        }
        dao.updateUsedFromSheet(perk.id, checked)
    }
}

enum class ToggleSyncResult {
    SyncedToSheet,
    LocalOnly
}

private fun updateSheetViaWebhook(
    webhookUrl: String,
    sheetUrl: String,
    rowNumber: Int,
    checked: Boolean
) {
    val sheetId = Regex("/d/([a-zA-Z0-9-_]+)")
        .find(sheetUrl)
        ?.groupValues
        ?.getOrNull(1)
        ?: throw IllegalArgumentException("Could not parse sheet ID from URL")
    val gid = Regex("[?&]gid=([0-9]+)")
        .find(sheetUrl)
        ?.groupValues
        ?.getOrNull(1)
        ?: "0"
    val dateUsed = if (checked) {
        LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_USED_FORMAT, Locale.US))
    } else {
        ""
    }
    val body = """
        {"sheetId":"${jsonEscape(sheetId)}","gid":"${jsonEscape(gid)}","rowNumber":$rowNumber,"checked":$checked,"dateUsed":"${jsonEscape(dateUsed)}"}
    """.trimIndent()

    try {
        val connection = (URL(webhookUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connectTimeout = 10_000
            readTimeout = 10_000
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
        if (responseText.isNotBlank() && !responseText.contains("\"ok\":true")) {
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
