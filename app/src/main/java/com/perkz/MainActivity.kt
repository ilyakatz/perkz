package com.perkz

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val ALL_CARDS_FILTER = "All cards"
private const val ALL_STATUSES_FILTER = "All statuses"
private const val NO_CARD_FILTER = "No card"
private const val DATE_USED_FORMAT = "MMM d"
private const val DEFAULT_EXPIRING_SOON_DAYS = 7L
private const val MONTHLY_EXPIRING_SOON_DAYS = 5L
private const val QUARTERLY_EXPIRING_SOON_DAYS = 21L

private val ComponentActivity.dataStore by preferencesDataStore(name = "settings")
private val Application.dataStore by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel = ViewModelProvider(
            this,
            PerkViewModel.Factory(application)
        )[PerkViewModel::class.java]

        setContent {
            MaterialTheme {
                PerkScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun PerkScreen(viewModel: PerkViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var urlInput by remember(uiState.sheetUrl) { mutableStateOf(uiState.sheetUrl) }
    var webhookInput by remember(uiState.webhookUrl) { mutableStateOf(uiState.webhookUrl) }
    var selectedTab by remember { mutableStateOf(AppTab.Perks) }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Credit Card Perks",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTab == AppTab.Perks,
                    onClick = { selectedTab = AppTab.Perks },
                    label = { Text("Perks") }
                )
                FilterChip(
                    selected = selectedTab == AppTab.Settings,
                    onClick = { selectedTab = AppTab.Settings },
                    label = { Text("Settings") }
                )
            }

            when (selectedTab) {
                AppTab.Perks -> PerksTabContent(
                    uiState = uiState,
                    onCardSelect = viewModel::selectCard,
                    onStatusSelect = viewModel::selectStatusFilter,
                    onToggleUsed = viewModel::toggleUsed
                )
                AppTab.Settings -> SettingsTabContent(
                    urlInput = urlInput,
                    webhookInput = webhookInput,
                    onUrlChange = { urlInput = it },
                    onWebhookChange = { webhookInput = it },
                    onSave = { viewModel.saveSettings(urlInput, webhookInput) },
                    onRefresh = viewModel::refresh
                )
            }
        }
    }
}

@Composable
private fun PerksTabContent(
    uiState: UiState,
    onCardSelect: (String) -> Unit,
    onStatusSelect: (String) -> Unit,
    onToggleUsed: (PerkEntity, Boolean) -> Unit
) {
    if (uiState.sheetUrl.isBlank()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Welcome to Perkz!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "To get started, please add your Google Sheet CSV URL in the Settings tab.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    if (uiState.availableCards.isNotEmpty()) {
        Text(
            text = "Filter by card",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.availableCards.forEach { card ->
                FilterChip(
                    selected = card == uiState.selectedCard,
                    onClick = { onCardSelect(card) },
                    label = { Text(card) }
                )
            }
        }
    }

    if (uiState.availableStatusFilters.isNotEmpty()) {
        Text(
            text = "Filter by status",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.availableStatusFilters.forEach { statusFilter ->
                FilterChip(
                    selected = statusFilter == uiState.selectedStatusFilter,
                    onClick = { onStatusSelect(statusFilter) },
                    label = { Text(statusFilter) }
                )
            }
        }
    }

    if (uiState.isLoading) {
        CircularProgressIndicator()
    }

    if (!uiState.hasAnyPerks && !uiState.isLoading) {
        Text("No perks found yet. Add your sheet URL in Settings, then tap Refresh.")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        uiState.statusGroups.forEach { statusGroup ->
            item(key = "status-${statusGroup.status.name}") {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = statusGroup.status.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusGroup.status.titleColor
                )
            }
            if (statusGroup.intervalGroups.isEmpty()) {
                item(key = "empty-${statusGroup.status.name}") {
                    Text(
                        text = statusGroup.status.emptyText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                statusGroup.intervalGroups.forEach { intervalGroup ->
                    item(key = "interval-${statusGroup.status.name}-${intervalGroup.interval}") {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = intervalGroup.interval,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(items = intervalGroup.items, key = { it.perk.id }) { item ->
                        PerkRow(
                            item = item,
                            onCheckedChange = { checked ->
                                onToggleUsed(item.perk, checked)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTabContent(
    urlInput: String,
    webhookInput: String,
    onUrlChange: (String) -> Unit,
    onWebhookChange: (String) -> Unit,
    onSave: () -> Unit,
    onRefresh: () -> Unit
) {
    Text(
        text = "Sheet connection",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    Text(
        text = "Use the CSV export URL from your Google Sheet",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    val urlValidationResult = validateCsvUrl(urlInput)
    val isValidUrl = urlValidationResult.isValid
    
    OutlinedTextField(
        value = urlInput,
        onValueChange = onUrlChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Google Sheet CSV URL") },
        supportingText = {
            if (!isValidUrl && urlInput.isNotBlank()) {
                Text(
                    urlValidationResult.errorMessage,
                    color = Color(0xFFB3261E)
                )
            }
        },
        isError = !isValidUrl && urlInput.isNotBlank()
    )
    OutlinedTextField(
        value = webhookInput,
        onValueChange = onWebhookChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Update webhook URL (Apps Script)") }
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onSave,
            enabled = isValidUrl
        ) {
            Text("Save settings")
        }
        TextButton(onClick = onRefresh) {
            Text("Refresh")
        }
    }
}

private enum class AppTab {
    Perks,
    Settings
}

@Composable
private fun PerkRow(item: UiPerkItem, onCheckedChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = item.status.cardColor)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isUsedThisPeriod,
                onCheckedChange = onCheckedChange
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = item.status.badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = item.status.titleColor
                )
                Text(item.perk.title, fontWeight = FontWeight.SemiBold)
                if (item.perk.card.isNotBlank()) {
                    Text(item.perk.card, style = MaterialTheme.typography.bodyMedium)
                }
                if (item.perk.maxValueOrUses.isNotBlank()) {
                    Text("Value: ${item.perk.maxValueOrUses}", style = MaterialTheme.typography.bodySmall)
                }
                if (item.resetPeriodLabel.isNotBlank()) {
                    Text("Reset: ${item.resetPeriodLabel}", style = MaterialTheme.typography.bodySmall)
                }
                if (item.perk.deadlineTrigger.isNotBlank()) {
                    Text("Deadline: ${item.perk.deadlineTrigger}", style = MaterialTheme.typography.bodySmall)
                }
                Text("Tracking period: ${item.periodLabel}", style = MaterialTheme.typography.bodySmall)
                if (item.perk.details.isNotBlank()) {
                    Text(item.perk.details, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Entity(tableName = "perks")
data class PerkEntity(
    @PrimaryKey val id: String,
    val title: String,
    val card: String,
    val interval: String,
    val sourceRowNumber: Int,
    val resetPeriod: String,
    val deadlineTrigger: String,
    val maxValueOrUses: String,
    val details: String,
    val usedFromSheet: Boolean
)

@Entity(tableName = "usage", primaryKeys = ["perkId", "periodKey"])
data class UsageEntity(
    val perkId: String,
    val periodKey: String
)

@Dao
interface PerkDao {
    @Query("SELECT * FROM perks ORDER BY interval, card, title")
    fun observePerks(): Flow<List<PerkEntity>>

    @Query("SELECT * FROM usage")
    fun observeUsage(): Flow<List<UsageEntity>>

    @Query("DELETE FROM perks")
    suspend fun clearPerks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerks(items: List<PerkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsage(usage: UsageEntity)

    @Query("DELETE FROM usage WHERE perkId = :perkId AND periodKey = :periodKey")
    suspend fun deleteUsage(perkId: String, periodKey: String)

    @Query("UPDATE perks SET usedFromSheet = :usedFromSheet WHERE id = :perkId")
    suspend fun updateUsedFromSheet(perkId: String, usedFromSheet: Boolean)
}

@Database(entities = [PerkEntity::class, UsageEntity::class], version = 6, exportSchema = false)
abstract class PerkDatabase : RoomDatabase() {
    abstract fun perkDao(): PerkDao
}

class PerkRepository(
    private val dao: PerkDao
) {
    fun observePerks(): Flow<List<PerkEntity>> = dao.observePerks()

    fun observeUsage(): Flow<List<UsageEntity>> = dao.observeUsage()

    suspend fun refresh(sheetUrl: String) {
        val csv = withContext(Dispatchers.IO) { URL(sheetUrl).readText() }
        val parsedPerks = parsePerksFromCsv(csv)
        dao.clearPerks()
        if (parsedPerks.isNotEmpty()) {
            dao.insertPerks(parsedPerks)
        }
    }

    suspend fun setUsed(
        perk: PerkEntity,
        checked: Boolean,
        sheetUrl: String,
        webhookUrl: String
    ): ToggleSyncResult {
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

        val periodKey = periodKeyFor(perk, LocalDate.now())
        if (checked) {
            dao.upsertUsage(UsageEntity(perkId = perk.id, periodKey = periodKey))
        } else {
            dao.deleteUsage(perk.id, periodKey)
        }
        dao.updateUsedFromSheet(perk.id, checked)
        return if (hasWebhook) ToggleSyncResult.SyncedToSheet else ToggleSyncResult.LocalOnly
    }
}

enum class ToggleSyncResult {
    SyncedToSheet,
    LocalOnly
}

class PerkViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        PerkDatabase::class.java,
        "perkz.db"
    )
        .fallbackToDestructiveMigration()
        .build()
    private val dao = db.perkDao()
    private val sheetUrlKey: Preferences.Key<String> = stringPreferencesKey("sheet_url")
    private val webhookUrlKey: Preferences.Key<String> = stringPreferencesKey("webhook_url")
    private val messageFlow = MutableStateFlow<String?>(null)
    private val loadingFlow = MutableStateFlow(false)
    private val selectedCardFlow = MutableStateFlow(ALL_CARDS_FILTER)
    private val selectedStatusFilterFlow = MutableStateFlow(ALL_STATUSES_FILTER)
    private val filtersFlow = combine(selectedCardFlow, selectedStatusFilterFlow) { selectedCard, selectedStatus ->
        selectedCard to selectedStatus
    }
    private val statusFlow = combine(loadingFlow, messageFlow) { loading, message ->
        loading to message
    }

    private val sheetUrlFlow: Flow<String> = application.dataStore.data.map {
        it[sheetUrlKey] ?: ""
    }
    private val webhookUrlFlow: Flow<String> = application.dataStore.data.map {
        it[webhookUrlKey] ?: ""
    }
    private val settingsFlow = combine(sheetUrlFlow, webhookUrlFlow) { sheetUrl, webhookUrl ->
        sheetUrl to webhookUrl
    }

    private val repository = PerkRepository(dao = dao)

    val uiState = combine(
        settingsFlow,
        repository.observePerks(),
        repository.observeUsage(),
        filtersFlow,
        statusFlow
    ) { settings, perks, usage, filters, status ->
        val (sheetUrl, webhookUrl) = settings
        val (selectedCard, selectedStatusFilter) = filters
        val (loading, message) = status
        val today = LocalDate.now()
        val usageKeys = usage.map { it.perkId to it.periodKey }.toSet()
        val items = perks.map { perk ->
            val key = periodKeyFor(perk, today)
            val used = usageKeys.contains(perk.id to key) || perk.usedFromSheet
            val statusItem = when {
                used -> PerkStatus.Used
                isExpiringSoon(perk, today) -> PerkStatus.ExpiringSoon
                else -> PerkStatus.NeedsUse
            }
            UiPerkItem(
                perk = perk,
                isUsedThisPeriod = used,
                periodLabel = periodLabelFor(perk, today),
                resetPeriodLabel = perk.resetPeriod.trim(),
                status = statusItem
            )
        }
        val availableCards = listOf(ALL_CARDS_FILTER) + items
            .map { cardLabelForFilter(it.perk.card) }
            .distinct()
            .sorted()
        val effectiveSelectedCard = selectedCard.takeIf { it in availableCards } ?: ALL_CARDS_FILTER
        val filteredItems = if (effectiveSelectedCard == ALL_CARDS_FILTER) {
            items
        } else {
            items.filter { cardLabelForFilter(it.perk.card) == effectiveSelectedCard }
        }
        val availableStatusFilters = listOf(ALL_STATUSES_FILTER) + PerkStatus.entries.map { it.label }
        val effectiveSelectedStatus =
            selectedStatusFilter.takeIf { it in availableStatusFilters } ?: ALL_STATUSES_FILTER
        val statusFilteredItems = if (effectiveSelectedStatus == ALL_STATUSES_FILTER) {
            filteredItems
        } else {
            filteredItems.filter { it.status.label == effectiveSelectedStatus }
        }
        val statusesToShow = if (effectiveSelectedStatus == ALL_STATUSES_FILTER) {
            PerkStatus.entries
        } else {
            PerkStatus.entries.filter { it.label == effectiveSelectedStatus }
        }
        val statusGroups = statusesToShow.map { statusValue ->
            val byStatus = statusFilteredItems.filter { it.status == statusValue }
            val intervalGroups = byStatus
                .groupBy { prettyInterval(it.perk.interval) }
                .toList()
                .sortedBy { it.first }
                .map { (interval, groupedItems) ->
                    UiIntervalGroup(interval = interval, items = groupedItems.sortedBy { it.perk.title })
                }
            UiStatusGroup(status = statusValue, intervalGroups = intervalGroups)
        }
        UiState(
            sheetUrl = sheetUrl,
            webhookUrl = webhookUrl,
            statusGroups = statusGroups,
            items = statusFilteredItems,
            hasAnyPerks = filteredItems.isNotEmpty(),
            availableCards = availableCards,
            selectedCard = effectiveSelectedCard,
            availableStatusFilters = availableStatusFilters,
            selectedStatusFilter = effectiveSelectedStatus,
            isLoading = loading,
            message = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState()
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val url = uiState.value.sheetUrl
            if (url.isBlank()) {
                messageFlow.value = "Sheet URL is not set. Please enter it in Settings."
                return@launch
            }
            loadingFlow.value = true
            runCatching {
                repository.refresh(url)
            }.onFailure {
                messageFlow.value = "Refresh failed: ${it.message ?: "unknown error"}"
            }
            loadingFlow.value = false
        }
    }

    fun saveSettings(url: String, webhookUrl: String) {
        viewModelScope.launch {
            val sanitized = url.trim()
            val sanitizedWebhook = webhookUrl.trim()
            getApplication<Application>().dataStore.edit {
                it[sheetUrlKey] = sanitized
                it[webhookUrlKey] = sanitizedWebhook
            }
            messageFlow.value = if (sanitized.isBlank()) "Settings saved. Please enter a sheet URL to use the app." else "Settings saved."
        }
    }

    fun toggleUsed(perk: PerkEntity, checked: Boolean) {
        viewModelScope.launch {
            runCatching {
                repository.setUsed(
                    perk = perk,
                    checked = checked,
                    sheetUrl = uiState.value.sheetUrl,
                    webhookUrl = uiState.value.webhookUrl
                )
            }.onSuccess { result ->
                if (result == ToggleSyncResult.LocalOnly) {
                    messageFlow.value = "Updated locally only. Add webhook URL in Settings to sync to Google Sheet."
                }
            }.onFailure {
                messageFlow.value = "Could not update: ${it.message ?: "unknown error"}"
            }
        }
    }

    fun clearMessage() {
        messageFlow.value = null
    }

    fun selectCard(card: String) {
        selectedCardFlow.value = card
    }

    fun selectStatusFilter(filter: String) {
        selectedStatusFilterFlow.value = filter
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return PerkViewModel(app) as T
        }
    }
}

data class UiState(
    val sheetUrl: String = "",
    val webhookUrl: String = "",
    val statusGroups: List<UiStatusGroup> = emptyList(),
    val items: List<UiPerkItem> = emptyList(),
    val hasAnyPerks: Boolean = false,
    val availableCards: List<String> = emptyList(),
    val selectedCard: String = ALL_CARDS_FILTER,
    val availableStatusFilters: List<String> = emptyList(),
    val selectedStatusFilter: String = ALL_STATUSES_FILTER,
    val isLoading: Boolean = false,
    val message: String? = null
)

data class UiStatusGroup(
    val status: PerkStatus,
    val intervalGroups: List<UiIntervalGroup>
)

data class UiIntervalGroup(
    val interval: String,
    val items: List<UiPerkItem>
)

data class UiPerkItem(
    val perk: PerkEntity,
    val isUsedThisPeriod: Boolean,
    val periodLabel: String,
    val resetPeriodLabel: String,
    val status: PerkStatus
)

enum class PerkStatus(
    val label: String,
    val badgeText: String,
    val titleColor: Color,
    val cardColor: Color,
    val emptyText: String
) {
    ExpiringSoon(
        label = "Expiring soon",
        badgeText = "EXPIRING SOON",
        titleColor = Color(0xFFB3261E),
        cardColor = Color(0xFFFFEDEB),
        emptyText = "Nothing expiring soon right now."
    ),
    NeedsUse(
        label = "Needs use",
        badgeText = "NEEDS USE",
        titleColor = Color(0xFF3B3B3B),
        cardColor = Color(0xFFF1EEF4),
        emptyText = "No pending perks in this section."
    ),
    Used(
        label = "Already used",
        badgeText = "USED",
        titleColor = Color(0xFF1B5E20),
        cardColor = Color(0xFFE8F5E9),
        emptyText = "No perks marked used yet."
    )
}

data class UrlValidationResult(
    val isValid: Boolean,
    val errorMessage: String = ""
)

private fun validateCsvUrl(url: String): UrlValidationResult {
    if (url.isBlank()) {
        return UrlValidationResult(isValid = true)
    }
    
    if (!url.contains("docs.google.com/spreadsheets")) {
        return UrlValidationResult(
            isValid = false,
            errorMessage = "URL must be a Google Sheets link"
        )
    }
    
    if (!url.contains("/export")) {
        return UrlValidationResult(
            isValid = false,
            errorMessage = "URL must be a CSV export URL (add /export?format=csv)"
        )
    }
    
    if (!url.contains("format=csv")) {
        return UrlValidationResult(
            isValid = false,
            errorMessage = "URL must have format=csv parameter"
        )
    }
    
    return UrlValidationResult(isValid = true)
}

private fun parsePerksFromCsv(csv: String): List<PerkEntity> {
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
    var titleIndex = findHeaderIndex(header, setOf("name", "title", "perk", "benefit", "description"))
        .takeIf { it >= 0 } ?: 0
    val cardIndex = findHeaderIndex(header, setOf("card", "cardname"))
        .takeIf { it >= 0 } ?: 1
    if (titleIndex == cardIndex) {
        titleIndex = when {
            cardIndex == 0 && header.size > 1 -> 1
            else -> header.indices.firstOrNull { it != cardIndex } ?: titleIndex
        }
    }
    val intervalIndex = findHeaderIndex(header, setOf("interval", "frequency", "cadence"))
        .takeIf { it >= 0 } ?: 2
    val resetPeriodIndex = findHeaderIndex(header, setOf("resetperiod", "periodwindow"))
        .takeIf { it >= 0 } ?: 3
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

private fun prettyInterval(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return "Monthly"
    return value.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
}

private fun cardLabelForFilter(rawCard: String): String {
    val trimmed = rawCard.trim()
    return if (trimmed.isBlank()) NO_CARD_FILTER else trimmed
}

private fun periodKeyFor(perk: PerkEntity, date: LocalDate): String {
    periodKeyFromResetPeriod(perk.resetPeriod, date)?.let { return it }
    return periodKeyForInterval(perk.interval, date)
}

private fun periodKeyForInterval(interval: String, date: LocalDate): String {
    val normalized = interval.lowercase(Locale.US).replace("-", "").replace(" ", "")
    return when {
        normalized.contains("month") -> date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
        normalized.contains("quarter") -> "${date.year}-Q${((date.monthValue - 1) / 3) + 1}"
        normalized.contains("semiannual") || normalized.contains("biannual") || normalized.contains("6month") ->
            "${date.year}-H${if (date.monthValue <= 6) 1 else 2}"
        normalized.contains("year") || normalized.contains("annual") -> date.year.toString()
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
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

private fun periodLabelFor(perk: PerkEntity, date: LocalDate): String {
    return periodKeyFor(perk, date)
}

private fun isExpiringSoon(perk: PerkEntity, date: LocalDate): Boolean {
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

private fun isMarkedUsedInSheet(usedValue: String, dateUsedValue: String): Boolean {
    if (dateUsedValue.trim().isNotBlank()) return true
    val rawUsed = usedValue.trim()
    if (rawUsed.isBlank()) return false
    val normalized = rawUsed.lowercase(Locale.US)
    if (normalized == "n/a" || normalized == "na") return false
    val numeric = rawUsed.replace(",", "").toDoubleOrNull()
    return numeric?.let { it > 0.0 } ?: true
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
    val dateUsed = if (checked) LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern(DATE_USED_FORMAT, Locale.US)) else ""
    val body = """
        {"sheetId":"${jsonEscape(sheetId)}","gid":"${jsonEscape(gid)}","rowNumber":$rowNumber,"checked":$checked,"dateUsed":"${jsonEscape(dateUsed)}"}
    """.trimIndent()
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
}

private fun jsonEscape(input: String): String {
    return input
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
