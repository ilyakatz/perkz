package com.perkz.ui.model

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.perkz.ui.theme.StatusExpiringSoonAccentDark
import com.perkz.ui.theme.StatusExpiringSoonCardDark
import com.perkz.ui.theme.StatusNeedsUseAccentDark
import com.perkz.ui.theme.StatusNeedsUseCardDark
import com.perkz.ui.theme.StatusUsedAccentDark
import com.perkz.ui.theme.StatusUsedCardDark

enum class PerkStatus(
    val label: String,
    val badgeText: String,
    /** Light-mode title / section-header color. */
    val titleColor: Color,
    /** Light-mode card background. */
    val cardColor: Color,
    /** Light-mode accent (bar, badge, deadline). */
    val accentColor: Color,
    val emptyText: String,
    val darkCardColor: Color,
    val darkAccentColor: Color,
) {
    ExpiringSoon(
        label = "Expiring soon",
        badgeText = "EXPIRING SOON",
        titleColor = Color(0xFFBA1A1A),
        cardColor = Color(0xFFFFF0EE),
        accentColor = Color(0xFFBA1A1A),
        emptyText = "Nothing expiring soon right now.",
        darkCardColor = StatusExpiringSoonCardDark,
        darkAccentColor = StatusExpiringSoonAccentDark,
    ),
    NeedsUse(
        label = "Needs use",
        badgeText = "NEEDS USE",
        titleColor = Color(0xFF1A237E),
        cardColor = Color(0xFFEEF0FF),
        accentColor = Color(0xFF4355B9),
        emptyText = "No pending perks in this section.",
        darkCardColor = StatusNeedsUseCardDark,
        darkAccentColor = StatusNeedsUseAccentDark,
    ),
    Used(
        label = "Already used",
        badgeText = "USED",
        titleColor = Color(0xFF1B5E20),
        cardColor = Color(0xFFE8F5E9),
        accentColor = Color(0xFF2E7D32),
        emptyText = "No perks marked used yet.",
        darkCardColor = StatusUsedCardDark,
        darkAccentColor = StatusUsedAccentDark,
    )
}

/** Theme-aware resolved colors for a [PerkStatus]. */
data class PerkStatusColors(
    val cardColor: Color,
    val accentColor: Color,
    val titleColor: Color,
)

@Composable
fun PerkStatus.resolvedColors(): PerkStatusColors {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) {
        PerkStatusColors(
            cardColor = darkCardColor,
            accentColor = darkAccentColor,
            titleColor = darkAccentColor,
        )
    } else {
        PerkStatusColors(
            cardColor = cardColor,
            accentColor = accentColor,
            titleColor = titleColor,
        )
    }
}
