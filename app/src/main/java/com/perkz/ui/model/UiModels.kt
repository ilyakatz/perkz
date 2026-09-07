package com.perkz.ui.model

import com.perkz.data.db.PerkEntity

internal const val ALL_CARDS_FILTER = "All cards"
internal const val ALL_STATUSES_FILTER = "All statuses"
internal const val ATTENTION_FILTER = "Attention"
internal val DEFAULT_STATUS_FILTERS = setOf(PerkStatus.ExpiringSoon, PerkStatus.NeedsUse)

data class UiState(
    val sheetUrl: String = "",
    val webhookUrl: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val statusGroups: List<UiStatusGroup> = emptyList(),
    val collapsedStatuses: Set<PerkStatus> = emptySet(),
    val collapsedIntervals: Set<String> = emptySet(),
    val allItems: List<UiPerkItem> = emptyList(),
    val items: List<UiPerkItem> = emptyList(),
    val hasAnyPerks: Boolean = false,
    val availableCards: List<String> = emptyList(),
    val cardCounts: Map<String, Int> = emptyMap(),
    val selectedCards: Set<String> = emptySet(),
    val statusCounts: Map<PerkStatus, Int> = emptyMap(),
    val selectedStatuses: Set<PerkStatus> = DEFAULT_STATUS_FILTERS,
    val isLoading: Boolean = false,
    val syncLabel: String = "Not synced yet",
    val message: String? = null,
    val syncError: String? = null
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
    val usedAmount: Double,
    val maxAmount: Double?,
    val periodLabel: String,
    val resetPeriodLabel: String,
    val status: PerkStatus,
    /** Friendly current-period range for display only, e.g. "Sep 1 - 30". */
    val periodRangeLabel: String = periodLabel
)
