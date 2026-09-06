package com.perkz.ui.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FilterSelectionUtilsTest {

    private data class Item(val card: String, val status: PerkStatus)

    @Test
    fun normalizeCardFilters_removesUnavailableAndTreatsAllSelectedAsAll() {
        val available = setOf("Amex Gold", "Chase Sapphire")
        assertEquals(
            setOf("Amex Gold"),
            normalizeCardFilters(setOf("Amex Gold", "Unknown"), available)
        )
        assertEquals(
            emptySet<String>(),
            normalizeCardFilters(setOf("Amex Gold", "Chase Sapphire"), available)
        )
    }

    @Test
    fun statusPresetLabel_maps_default_all_single_and_custom() {
        assertEquals(ATTENTION_FILTER, statusPresetLabelFor(DEFAULT_STATUS_FILTERS))
        assertEquals(ALL_STATUSES_FILTER, statusPresetLabelFor(emptySet()))
        assertEquals("Expired", statusPresetLabelFor(setOf(PerkStatus.Expired)))
        assertEquals("2 statuses", statusPresetLabelFor(setOf(PerkStatus.Expired, PerkStatus.Upcoming)))
    }

    @Test
    fun applyPerkFilters_applies_cards_then_statuses() {
        val items = listOf(
            Item("Amex Gold", PerkStatus.ExpiringSoon),
            Item("Amex Gold", PerkStatus.Used),
            Item("Chase Sapphire", PerkStatus.NeedsUse)
        )

        val filtered = applyPerkFilters(
            items = items,
            selectedCards = setOf("Amex Gold"),
            selectedStatuses = setOf(PerkStatus.ExpiringSoon),
            cardSelector = Item::card,
            statusSelector = Item::status
        )

        assertEquals(listOf(items.first()), filtered)
    }

    @Test
    fun encodeDecode_roundTrips_for_cards_and_statuses() {
        val cards = setOf("Amex Gold", "Chase Sapphire")
        val statuses = setOf(PerkStatus.Expired, PerkStatus.Upcoming)
        assertEquals(cards, decodeStringSet(encodeStringSet(cards)))
        assertEquals(statuses, decodeStatusSet(encodeStatusSet(statuses)))
    }
}
