package com.perkz.ui.model

import androidx.compose.ui.graphics.Color

enum class PerkStatus(
    val label: String,
    val badgeText: String,
    val titleColor: Color,
    val cardColor: Color,
    val accentColor: Color,
    val emptyText: String
) {
    ExpiringSoon(
        label = "Expiring soon",
        badgeText = "EXPIRING SOON",
        titleColor = Color(0xFFBA1A1A),
        cardColor = Color(0xFFFFF0EE),
        accentColor = Color(0xFFBA1A1A),
        emptyText = "Nothing expiring soon right now."
    ),
    NeedsUse(
        label = "Needs use",
        badgeText = "NEEDS USE",
        titleColor = Color(0xFF1A237E),
        cardColor = Color(0xFFEEF0FF),
        accentColor = Color(0xFF4355B9),
        emptyText = "No pending perks in this section."
    ),
    Used(
        label = "Already used",
        badgeText = "USED",
        titleColor = Color(0xFF1B5E20),
        cardColor = Color(0xFFE8F5E9),
        accentColor = Color(0xFF2E7D32),
        emptyText = "No perks marked used yet."
    )
}
