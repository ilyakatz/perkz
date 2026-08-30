package com.perkz.viewmodel

import android.app.Application
import android.util.Log
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
import com.perkz.domain.isExpiringSoon
import com.perkz.domain.periodKeyFor
import com.perkz.domain.periodLabelFor
import com.perkz.domain.prettyInterval
import com.perkz.ui.model.ALL_CARDS_FILTER
import com.perkz.ui.model.ALL_STATUSES_FILTER
import com.perkz.ui.model.PerkStatus
import com.perkz.ui.model.ThemeMode
import com.perkz.ui.model.UiIntervalGroup
import com.perkz.ui.model.UiPerkItem
import com.perkz.ui.model.UiState
import com.perkz.ui.model.UiStatusGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private val Application.dataStore by preferencesDataStore(name = "settings")

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
    private val selectedCardKey: Preferences.Key<String> = stringPreferencesKey("selected_card")
    private val selectedStatusFilterKey: Preferences.Key<String> = stringPreferencesKey("selected_status_filter")
    private val themeModeKey: Preferences.Key<String> = stringPreferencesKey("theme_mode")
    private val messageFlow = MutableStateFlow<String?>(null)
    private val loadingFlow = MutableStateFlow(false)
    private val selectedCardFlow = application.dataStore.data.map {
        it[selectedCardKey] ?: ALL_CARDS_FILTER
    }.stateIn(viewModelScope, SharingStarted.Lazily, ALL_CARDS_FILTER)
    private val selectedStatusFilterFlow = application.dataStore.data.map {
        it[selectedStatusFilterKey] ?: ALL_STATUSES_FILTER
    }.stateIn(viewModelScope, SharingStarted.Lazily, ALL_STATUSES_FILTER)
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
    private val themeModeFlow: Flow<ThemeMode> = application.dataStore.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[themeModeKey] } ?: ThemeMode.SYSTEM
    }
    private val settingsFlow = combine(sheetUrlFlow, webhookUrlFlow, themeModeFlow) { sheetUrl, webhookUrl, themeMode ->
        Triple(sheetUrl, webhookUrl, themeMode)
    }

    private val repository = PerkRepository(dao = dao)

    val uiState = combine(
        settingsFlow,
        repository.observePerks(),
        repository.observeUsage(),
        filtersFlow,
        statusFlow
    ) { settings, perks, usage, filters, status ->
        val (sheetUrl, webhookUrl, themeMode) = settings
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
            themeMode = themeMode,
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
                messageFlow.value = "Sheet URL is not set. Please enter it in Settings."
                Log.w("PerkViewModel", "Refresh attempted without URL")
                return@launch
            }
            loadingFlow.value = true
            try {
                Log.d("PerkViewModel", "Starting refresh with URL: $url")
                repository.refresh(url)
                messageFlow.value = "Perks refreshed successfully! Check the data above."
                Log.i("PerkViewModel", "Refresh completed successfully")
            } catch (e: Exception) {
                val errorMsg = "Refresh failed: ${e.message ?: "unknown error"}"
                messageFlow.value = errorMsg
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

    fun saveThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit {
                it[themeModeKey] = mode.name
            }
        }
    }

    fun selectCard(card: String) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit {
                it[selectedCardKey] = card
            }
        }
    }

    fun selectStatusFilter(filter: String) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit {
                it[selectedStatusFilterKey] = filter
            }
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return PerkViewModel(app) as T
        }
    }
}
