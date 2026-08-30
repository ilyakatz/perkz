package com.perkz.ui.model

import androidx.compose.ui.graphics.Color

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
