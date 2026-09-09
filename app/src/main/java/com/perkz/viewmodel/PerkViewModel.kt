package com.perkz.viewmodel

import android.app.Application
import android.util.Log
import androidx.core.text.HtmlCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.perkz.data.db.PerkDatabase
import com.perkz.data.db.PerkEntity
import com.perkz.data.repository.PerkRepository
import com.perkz.data.repository.ToggleSyncResult
import com.perkz.domain.cardLabelForFilter
import com.perkz.domain.parseAmount
import com.perkz.domain.periodKeyFor
import com.perkz.domain.periodLabelFor
import com.perkz.domain.periodRangeLabelFor
import com.perkz.domain.prettyInterval
import com.perkz.domain.classifyPerkStatus
import com.perkz.domain.usageAmountFor
import com.perkz.ui.model.ALL_STATUSES_FILTER
import com.perkz.ui.model.ATTENTION_FILTER
import com.perkz.ui.model.DEFAULT_STATUS_FILTERS
import com.perkz.ui.model.NotificationSchedule
import com.perkz.ui.model.PerkStatus
import com.perkz.ui.model.ThemeMode
import com.perkz.ui.model.UiIntervalGroup
import com.perkz.ui.model.UiPerkItem
import com.perkz.ui.model.UiState
import com.perkz.ui.model.UiStatusGroup
import com.perkz.ui.model.applyPerkFilters
import com.perkz.ui.model.decodeStatusSet
import com.perkz.ui.model.decodeStringSet
import com.perkz.ui.model.encodeStatusSet
import com.perkz.ui.model.encodeStringSet
import com.perkz.ui.model.isAllStatusesSelection
import com.perkz.ui.model.normalizeCardFilters
import com.perkz.ui.model.normalizeStatusFilters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.perkz.worker.PerkReminderWorker
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

private val Application.dataStore by preferencesDataStore(name = "settings")

private data class SyncStatus(
    val loading: Boolean,
    val message: String?,
    val error: String?,
    val label: String
)

class PerkViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        PerkDatabase::class.java,
        "perkz.db"
    )
        .addMigrations(PerkDatabase.MIGRATION_9_10, PerkDatabase.MIGRATION_10_11)
        .fallbackToDestructiveMigration()
        .build()
    private val dao = db.perkDao()
    private val sheetUrlKey: Preferences.Key<String> = stringPreferencesKey("sheet_url")
    private val webhookUrlKey: Preferences.Key<String> = stringPreferencesKey("webhook_url")
    private val selectedCardsKey: Preferences.Key<String> = stringPreferencesKey("selected_cards")
    private val selectedStatusesKey: Preferences.Key<String> = stringPreferencesKey("selected_statuses")
    private val selectedCardKey: Preferences.Key<String> = stringPreferencesKey("selected_card")
    private val selectedStatusFilterKey: Preferences.Key<String> = stringPreferencesKey("selected_status_filter")
    private val themeModeKey: Preferences.Key<String> = stringPreferencesKey("theme_mode")
    private val notificationScheduleKey: Preferences.Key<String> = stringPreferencesKey("notification_schedule")
    private val notificationTimeKey: Preferences.Key<String> = stringPreferencesKey("notification_time")
    private val collapsedStatusesKey: Preferences.Key<String> = stringPreferencesKey("collapsed_statuses")
    private val collapsedIntervalsKey: Preferences.Key<String> = stringPreferencesKey("collapsed_intervals")
    private val messageFlow = MutableStateFlow<String?>(null)
    private val syncErrorFlow = MutableStateFlow<String?>(null)
    private val loadingFlow = MutableStateFlow(false)
    private val syncTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    private val selectedCardsFlow = application.dataStore.data.map { prefs ->
        prefs[selectedCardsKey]?.let(::decodeStringSet)
            ?: prefs[selectedCardKey]
                ?.takeUnless { it == com.perkz.ui.model.ALL_CARDS_FILTER }
                ?.let(::setOf)
                .orEmpty()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptySet())
    private val selectedStatusesFlow = application.dataStore.data.map { prefs ->
        prefs[selectedStatusesKey]?.let(::decodeStatusSet) ?: legacyStatusSelection(prefs[selectedStatusFilterKey])
    }.stateIn(viewModelScope, SharingStarted.Lazily, DEFAULT_STATUS_FILTERS)
    private val filtersFlow = combine(selectedCardsFlow, selectedStatusesFlow) { selectedCards, selectedStatuses ->
        selectedCards to selectedStatuses
    }

    private val sheetUrlFlow: Flow<String> = application.dataStore.data.map {
        it[sheetUrlKey] ?: ""
    }.distinctUntilChanged()
    private val webhookUrlFlow: Flow<String> = application.dataStore.data.map {
        it[webhookUrlKey] ?: ""
    }
    private val themeModeFlow: Flow<ThemeMode> = application.dataStore.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[themeModeKey] } ?: ThemeMode.SYSTEM
    }
    private val notificationScheduleFlow: Flow<NotificationSchedule> = application.dataStore.data.map { prefs ->
        NotificationSchedule.fromName(prefs[notificationScheduleKey])
    }
    private val notificationTimeFlow: Flow<LocalTime> = application.dataStore.data.map { prefs ->
        prefs[notificationTimeKey]?.let { LocalTime.parse(it) } ?: LocalTime.of(9, 0)
    }
    private val collapsedStatusesFlow: Flow<Set<PerkStatus>> = application.dataStore.data.map { prefs ->
        prefs[collapsedStatusesKey]
            .orEmpty()
            .split(',')
            .mapNotNull { value -> PerkStatus.entries.firstOrNull { it.name == value } }
            .toSet()
    }
    private val collapsedIntervalsFlow: Flow<Set<String>> = application.dataStore.data.map { prefs ->
        prefs[collapsedIntervalsKey].orEmpty().split(',').filter { it.isNotBlank() }.toSet()
    }
    private val baseSettingsFlow = combine(
        sheetUrlFlow, webhookUrlFlow, themeModeFlow, notificationScheduleFlow, notificationTimeFlow
    ) { sheetUrl, webhookUrl, themeMode, notificationSchedule, notificationTime ->
        BaseSettings(sheetUrl, webhookUrl, themeMode, notificationSchedule, notificationTime)
    }

    private data class BaseSettings(
        val sheetUrl: String,
        val webhookUrl: String,
        val themeMode: ThemeMode,
        val notificationSchedule: NotificationSchedule,
        val notificationTime: LocalTime
    )

    private val settingsFlow = combine(
        baseSettingsFlow, collapsedStatusesFlow, collapsedIntervalsFlow
    ) { base, collapsedStatuses, collapsedIntervals ->
        Triple(base, collapsedStatuses, collapsedIntervals)
    }

    private val repository = PerkRepository(dao = dao)
    private val syncStatusFlow = repository.observeSyncStatus()
    private val statusFlow = combine(loadingFlow, messageFlow, syncErrorFlow, syncStatusFlow) { loading, message, error, syncStatus ->
        val syncLabel = syncStatus?.lastSyncedAtEpochMillis?.let { timestamp ->
            val time = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .format(syncTimeFormatter)
            "Synced at $time"
        } ?: "Not synced yet"
        SyncStatus(loading, message, error, syncLabel)
    }

    val uiState = combine(
        settingsFlow,
        repository.observePerks(),
        repository.observeUsage(),
        filtersFlow,
        statusFlow
    ) { settings, perks, usage, filters, status ->
        val (baseSettings, collapsedStatuses, collapsedIntervals) = settings
        val (sheetUrl, webhookUrl, themeMode, notificationSchedule, notificationTime) = baseSettings
        val (selectedCards, selectedStatuses) = filters
        val loading = status.loading
        val message = status.message
        val syncError = status.error
        val syncLabel = status.label
        val today = LocalDate.now()
        val usageByKey = usage.associateBy { it.perkId to it.periodKey }
        val items = perks.map { perk ->
            val key = periodKeyFor(perk, today)
            val usageAmount = usageAmountFor(perk, usageByKey[perk.id to key]?.amount)
            val maxAmount = parseAmount(perk.maxValueOrUses)
            val used = usageAmount > 0.0
            val statusItem = classifyPerkStatus(perk, today, usageAmount)
            UiPerkItem(
                perk = perk,
                isUsedThisPeriod = used,
                usedAmount = usageAmount,
                maxAmount = maxAmount,
                periodLabel = periodLabelFor(perk, today),
                resetPeriodLabel = perk.resetPeriod.trim(),
                status = statusItem,
                periodRangeLabel = periodRangeLabelFor(perk, today)
            )
        }
        val availableCards = items
            .map { cardLabelForFilter(it.perk.card) }
            .distinct()
            .sorted()
        val effectiveSelectedCards = normalizeCardFilters(selectedCards, availableCards.toSet())
        val effectiveSelectedStatuses = normalizeStatusFilters(selectedStatuses)
        val visibleItems = applyPerkFilters(
            items = items,
            selectedCards = effectiveSelectedCards,
            selectedStatuses = effectiveSelectedStatuses,
            cardSelector = { cardLabelForFilter(it.perk.card) },
            statusSelector = { it.status }
        )
        val statusesToShow = if (effectiveSelectedStatuses.isAllStatusesSelection()) {
            PerkStatus.entries
        } else {
            PerkStatus.entries.filter { it in effectiveSelectedStatuses }
        }
        val statusGroups = statusesToShow.map { statusValue ->
            val byStatus = visibleItems.filter { it.status == statusValue }
            val intervalGroups = byStatus
                .groupBy { prettyInterval(it.perk.interval) }
                .toList()
                .sortedWith(compareBy({ if (it.first.equals("Monthly", ignoreCase = true)) 0 else 1 }, { it.first }))
                .map { (interval, groupedItems) ->
                    UiIntervalGroup(interval = interval, items = groupedItems.sortedBy { it.perk.title })
                }
            UiStatusGroup(status = statusValue, intervalGroups = intervalGroups)
        }
        UiState(
            sheetUrl = sheetUrl,
            webhookUrl = webhookUrl,
            themeMode = themeMode,
            statusGroups = statusGroups,
            collapsedStatuses = collapsedStatuses,
            collapsedIntervals = collapsedIntervals,
            allItems = items,
            items = visibleItems,
            hasAnyPerks = visibleItems.isNotEmpty(),
            availableCards = availableCards,
            cardCounts = availableCards.associateWith { card ->
                items.count { cardLabelForFilter(it.perk.card) == card }
            },
            selectedCards = effectiveSelectedCards,
            statusCounts = PerkStatus.entries.associateWith { statusValue ->
                items.count { it.status == statusValue }
            },
            selectedStatuses = effectiveSelectedStatuses,
            isLoading = loading,
            syncLabel = if (loading) "Syncing…" else syncLabel,
            notificationSchedule = notificationSchedule,
            notificationTime = notificationTime,
            message = message,
            syncError = syncError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState()
    )

    fun saveNotificationSettings(schedule: NotificationSchedule, time: LocalTime) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[notificationScheduleKey] = schedule.name
                prefs[notificationTimeKey] = time.toString()
            }
            updateWorkerSchedule(schedule, time)
        }
    }

    fun triggerTestNotification() {
        val ui = uiState.value
        val expiringSoon = ui.allItems.filter { it.status == PerkStatus.ExpiringSoon }
        val notificationManager = com.perkz.notification.PerkNotificationManager(getApplication())
        
        if (expiringSoon.isEmpty()) {
            notificationManager.showNotification(
                "Perkz",
                "You're all caught up! No perks are expiring soon."
            )
        } else {
            expiringSoon.forEach { item ->
                val sb = StringBuilder()
                val today = LocalDate.now()
                val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, today.withDayOfMonth(today.lengthOfMonth()))
                
                sb.append("⏳ ${daysLeft.coerceAtLeast(0)} days left • ${item.perk.interval} benefit")
                
                val styledMessage = HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
                notificationManager.showNotification(
                    title = "${item.perk.title} (${item.perk.card})",
                    message = styledMessage,
                    notificationId = item.perk.sourceRowNumber
                )
            }
        }
    }

    private fun updateWorkerSchedule(schedule: NotificationSchedule, time: LocalTime) {
        val workManager = WorkManager.getInstance(getApplication())
        if (schedule == NotificationSchedule.Off) {
            workManager.cancelUniqueWork("perk_reminder")
        } else {
            val now = LocalDateTime.now()
            var target = now.with(time)
            if (target.isBefore(now)) {
                target = target.plusDays(1)
            }
            val initialDelay = java.time.Duration.between(now, target).toMillis()

            val repeatInterval = if (schedule == NotificationSchedule.Daily) 1L else 7L
            val workRequest = PeriodicWorkRequestBuilder<PerkReminderWorker>(
                repeatInterval, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "perk_reminder",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    init {
        // Automatically refresh when sheet URL becomes available
        viewModelScope.launch {
            sheetUrlFlow.collect { url ->
                if (url.isNotBlank()) {
                    repository.refresh(url)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val url = uiState.value.sheetUrl
            if (url.isBlank()) {
                val error = "Sheet URL is not set. Please enter it in Settings."
                syncErrorFlow.value = error
                messageFlow.value = error
                Log.w("PerkViewModel", "Refresh attempted without URL")
                return@launch
            }
            loadingFlow.value = true
            syncErrorFlow.value = null
            try {
                Log.d("PerkViewModel", "Starting refresh with URL: $url")
                repository.refresh(url)
                messageFlow.value = "Perks refreshed successfully! Check the data above."
                syncErrorFlow.value = null
                Log.i("PerkViewModel", "Refresh completed successfully")
            } catch (e: Exception) {
                val errorMsg = "Refresh failed: ${e.message ?: "unknown error"}"
                messageFlow.value = errorMsg
                syncErrorFlow.value = errorMsg
                Log.e("PerkViewModel", errorMsg, e)
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
            messageFlow.value = if (sanitized.isBlank()) {
                "Settings saved. Please enter a sheet URL to use the app."
            } else {
                "Settings saved."
            }
        }
    }

    fun toggleUsed(perk: PerkEntity, checked: Boolean) {
        viewModelScope.launch {
            try {
                repository.setUsed(
                    perk = perk,
                    checked = checked,
                    sheetUrl = uiState.value.sheetUrl,
                    webhookUrl = uiState.value.webhookUrl
                ).also { result ->
                    if (result == ToggleSyncResult.LocalOnly) {
                        messageFlow.value = "Updated locally only. Add webhook URL in Settings to sync to Google Sheet."
                    }
                }
            } catch (error: Exception) {
                repository.updateLocalUsed(perk, perk.usedFromSheet)
                messageFlow.value = "Could not update: ${error.message ?: "unknown error"}"
            }
        }
    }

    fun addUsage(perk: PerkEntity, amountToAdd: Double) {
        if (amountToAdd == 0.0) return
        viewModelScope.launch {
            try {
                val existingAmount = repository.currentUsageAmount(perk)
                val newAmount = (existingAmount + amountToAdd).coerceAtLeast(0.0)
                val result = repository.setUsedAmount(
                    perk = perk,
                    amount = newAmount,
                    sheetUrl = uiState.value.sheetUrl,
                    webhookUrl = uiState.value.webhookUrl
                )
                if (result == ToggleSyncResult.LocalOnly) {
                    messageFlow.value = "Updated locally only. Add webhook URL in Settings to sync to Google Sheet."
                }
            } catch (error: Exception) {
                messageFlow.value = "Could not update: ${error.message ?: "unknown error"}"
            }
        }
    }

        fun markFull(perk: PerkEntity) {
            viewModelScope.launch {
                runCatching {
                    repository.setUsed(
                        perk = perk,
                        checked = true,
                        sheetUrl = uiState.value.sheetUrl,
                        webhookUrl = uiState.value.webhookUrl
                    )
                }.onFailure { error ->
                    messageFlow.value = "Could not mark full: ${error.message ?: "unknown error"}"
                }
            }
        }

        fun clearUsage(perk: PerkEntity) {
            viewModelScope.launch {
                runCatching {
                    repository.setUsed(
                        perk = perk,
                        checked = false,
                        sheetUrl = uiState.value.sheetUrl,
                        webhookUrl = uiState.value.webhookUrl
                    )
                }.onFailure { error ->
                    messageFlow.value = "Could not clear usage: ${error.message ?: "unknown error"}"
                }
            }
        }

    fun setNotApplicable(perk: PerkEntity, notApplicable: Boolean) {
        viewModelScope.launch {
            try {
                repository.setNotApplicable(
                    perk = perk,
                    notApplicable = notApplicable,
                    sheetUrl = uiState.value.sheetUrl,
                    webhookUrl = uiState.value.webhookUrl
                ).also { result ->
                    if (result == ToggleSyncResult.LocalOnly) {
                        messageFlow.value = "Updated locally only. Add webhook URL in Settings to sync to Google Sheet."
                    }
                }
            } catch (error: Exception) {
                messageFlow.value = "Could not update: ${error.message ?: "unknown error"}"
            }
        }
    }

    fun clearMessage() {
        messageFlow.value = null
    }

    fun saveThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit {
                it[themeModeKey] = mode.name
            }
        }
    }

    fun selectCard(card: String) {
        val nextCards = if (card == com.perkz.ui.model.ALL_CARDS_FILTER) emptySet() else setOf(card)
        saveFilters(nextCards, uiState.value.selectedStatuses)
    }

    fun selectStatusPreset(statuses: Set<PerkStatus>) {
        saveFilters(uiState.value.selectedCards, statuses)
    }

    fun applyFilters(cards: Set<String>, statuses: Set<PerkStatus>) {
        saveFilters(cards, statuses)
    }

    fun clearFilters() {
        saveFilters(emptySet(), DEFAULT_STATUS_FILTERS)
    }

    fun toggleStatusCollapsed(status: PerkStatus) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { preferences ->
                val current = preferences[collapsedStatusesKey]
                    .orEmpty()
                    .split(',')
                    .mapNotNull { value -> PerkStatus.entries.firstOrNull { it.name == value } }
                    .toMutableSet()
                if (!current.add(status)) current.remove(status)
                preferences[collapsedStatusesKey] = current.joinToString(",") { it.name }
            }

        }
    }

    fun toggleIntervalCollapsed(interval: String) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { preferences ->
                val current = preferences[collapsedIntervalsKey].orEmpty()
                    .split(',').filter { it.isNotBlank() }.toMutableSet()
                if (!current.add(interval)) current.remove(interval)
                preferences[collapsedIntervalsKey] = current.joinToString(",")
            }

        }
    }

    fun setIntervalsCollapsed(intervals: Set<String>, collapsed: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { preferences ->
                val current = preferences[collapsedIntervalsKey].orEmpty()
                    .split(',').filter { it.isNotBlank() }.toMutableSet()
                if (collapsed) {
                    current.addAll(intervals)
                } else {
                    current.removeAll(intervals)
                }
                preferences[collapsedIntervalsKey] = current.joinToString(",")
            }
        }
    }

    private fun saveFilters(cards: Set<String>, statuses: Set<PerkStatus>) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { preferences ->
                val normalizedCards = normalizeCardFilters(cards, uiState.value.availableCards.toSet())
                preferences[selectedCardsKey] = encodeStringSet(normalizedCards)
                preferences[selectedStatusesKey] = encodeStatusSet(normalizeStatusFilters(statuses))
                preferences.remove(selectedCardKey)
                preferences.remove(selectedStatusFilterKey)
            }
        }
    }

    private fun legacyStatusSelection(value: String?): Set<PerkStatus> = when {
        value.isNullOrBlank() -> DEFAULT_STATUS_FILTERS
        value == ATTENTION_FILTER -> DEFAULT_STATUS_FILTERS
        value == ALL_STATUSES_FILTER -> emptySet()
        else -> PerkStatus.entries.firstOrNull { it.label == value }?.let { setOf(it) } ?: DEFAULT_STATUS_FILTERS
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return PerkViewModel(app) as T
        }
    }
}
