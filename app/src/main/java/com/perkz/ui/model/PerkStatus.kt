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
    /** Short helper copy shown under the section title when expanded. */
    val subtitle: String,
    /** Light-mode title / section-header color. */
    val titleColor: Color,
    /** Light-mode card background. */
    val cardColor: Color,
    /** Light-mode accent (bar, badge, deadline). */
    val accentColor: Color,
    val emptyText: String,
    val darkCardColor: Color,
    val darkAccentColor: Color,
    /** Whether a "remaining" value summary is meaningful for this section. */
    val showsRemainingSummary: Boolean = false,
) {
    ExpiringSoon(
        label = "Expiring soon",
        badgeText = "EXPIRING SOON",
        subtitle = "Perks that reset soon",
        titleColor = Color(0xFFBA1A1A),
        cardColor = Color(0xFFFFF0EE),
        accentColor = Color(0xFFBA1A1A),
        emptyText = "Nothing expiring soon right now.",
        darkCardColor = StatusExpiringSoonCardDark,
        darkAccentColor = StatusExpiringSoonAccentDark,
        showsRemainingSummary = true,
    ),
    NeedsUse(
        label = "Needs use",
        badgeText = "NEEDS USE",
        subtitle = "Perks with no usage recorded yet",
        titleColor = Color(0xFF1A237E),
        cardColor = Color(0xFFEEF0FF),
        accentColor = Color(0xFF4355B9),
        emptyText = "No pending perks in this section.",
        darkCardColor = StatusNeedsUseCardDark,
        darkAccentColor = StatusNeedsUseAccentDark,
        showsRemainingSummary = true,
    ),
    Upcoming(
        label = "Upcoming",
        badgeText = "UPCOMING",
        subtitle = "Perks starting in a future period",
        titleColor = Color(0xFF5E35B1),
        cardColor = Color(0xFFF3EEFF),
        accentColor = Color(0xFF7E57C2),
        emptyText = "No upcoming perks.",
        darkCardColor = StatusNeedsUseCardDark,
        darkAccentColor = StatusNeedsUseAccentDark,
        showsRemainingSummary = true,
    ),
    PartiallyUsed(
        label = "Partially used",
        badgeText = "PARTIALLY USED",
        subtitle = "Perks with some usage recorded",
        titleColor = Color(0xFF00695C),
        cardColor = Color(0xFFE8F5F3),
        accentColor = Color(0xFF00897B),
        emptyText = "No partially used perks.",
        darkCardColor = StatusUsedCardDark,
        darkAccentColor = StatusUsedAccentDark,
        showsRemainingSummary = true,
    ),
    Used(
        label = "Already used",
        badgeText = "USED",
        subtitle = "Perks fully used this period",
        titleColor = Color(0xFF1B5E20),
        cardColor = Color(0xFFE8F5E9),
        accentColor = Color(0xFF2E7D32),
        emptyText = "No perks marked used yet.",
        darkCardColor = StatusUsedCardDark,
        darkAccentColor = StatusUsedAccentDark,
    ),
    Expired(
        label = "Expired",
        badgeText = "EXPIRED",
        subtitle = "Perks past their deadline",
        titleColor = Color(0xFF6D4C41),
        cardColor = Color(0xFFF5EFED),
        accentColor = Color(0xFF6D4C41),
        emptyText = "No expired perks.",
        darkCardColor = StatusExpiringSoonCardDark,
        darkAccentColor = StatusExpiringSoonAccentDark,
    ),
    NotApplicable(
        label = "Not applicable",
        badgeText = "N/A",
        subtitle = "Perks marked not applicable",
        titleColor = Color(0xFF546E7A),
        cardColor = Color(0xFFECEFF1),
        accentColor = Color(0xFF607D8B),
        emptyText = "No perks marked not applicable.",
        darkCardColor = Color(0xFF263238),
        darkAccentColor = Color(0xFF90A4AE),
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
