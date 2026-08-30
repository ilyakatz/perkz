package com.perkz.ui.model

import com.perkz.data.db.PerkEntity

internal const val ALL_CARDS_FILTER = "All cards"
internal const val ALL_STATUSES_FILTER = "All statuses"

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
