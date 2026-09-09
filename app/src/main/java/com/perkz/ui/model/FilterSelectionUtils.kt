package com.perkz.ui.model

internal fun encodeStringSet(values: Set<String>): String =
    values.filter { it.isNotBlank() }.sorted().joinToString("|")

internal fun decodeStringSet(value: String): Set<String> =
    value.split('|').map { it.trim() }.filter { it.isNotBlank() }.toSet()

internal fun encodeStatusSet(values: Set<PerkStatus>): String =
    values.joinToString("|") { it.name }

internal fun decodeStatusSet(value: String): Set<PerkStatus> =
    value.split('|')
        .mapNotNull { raw -> PerkStatus.entries.firstOrNull { it.name == raw.trim() } }
        .toSet()

internal fun normalizeCardFilters(selected: Set<String>, availableCards: Set<String>): Set<String> {
    if (availableCards.isEmpty()) return selected.filter { it.isNotBlank() }.toSet()
    val valid = selected.filter { it in availableCards }.toSet()
    return if (valid.size == availableCards.size) emptySet() else valid
}

internal fun normalizeStatusFilters(selected: Set<PerkStatus>): Set<PerkStatus> =
    if (selected.size == PerkStatus.entries.size) emptySet() else selected

internal fun Set<PerkStatus>.isAllStatusesSelection(): Boolean =
    isEmpty() || size == PerkStatus.entries.size

internal fun statusPresetLabelFor(statuses: Set<PerkStatus>): String = when {
    statuses == DEFAULT_STATUS_FILTERS -> ATTENTION_FILTER
    statuses.isAllStatusesSelection() -> ALL_STATUSES_FILTER
    statuses.size == 1 -> statuses.first().label
    else -> "${statuses.size} statuses"
}

internal fun <T> applyPerkFilters(
    items: List<T>,
    selectedCards: Set<String>,
    selectedStatuses: Set<PerkStatus>,
    searchQuery: String = "",
    cardSelector: (T) -> String,
    statusSelector: (T) -> PerkStatus,
    searchSelector: (T) -> String = { "" }
): List<T> {
    val searchFiltered = if (searchQuery.isBlank()) {
        items
    } else {
        val query = searchQuery.trim().lowercase()
        items.filter { searchSelector(it).lowercase().contains(query) }
    }

    val cardsFiltered = if (selectedCards.isEmpty()) {
        searchFiltered
    } else {
        searchFiltered.filter { cardSelector(it) in selectedCards }
    }
    val effectiveStatuses = normalizeStatusFilters(selectedStatuses)
    return if (effectiveStatuses.isAllStatusesSelection()) {
        cardsFiltered
    } else {
        cardsFiltered.filter { statusSelector(it) in effectiveStatuses }
    }
}
