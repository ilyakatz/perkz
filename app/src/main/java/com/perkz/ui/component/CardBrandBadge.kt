package com.perkz.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact brand badge shown next to a card name.
 *
 * Maps well-known issuers (Amex, Chase, Capital One, Citi, Discover, Wells Fargo,
 * Bank of America, US Bank, Barclays, Apple, Synchrony, Navy Federal, Amazon,
 * Goldman Sachs) to branded background colours and initials. Unknown cards fall
 * back to the theme primary colour with up to two initials derived from the name.
 *
 * The badge is a 30 × 30 dp rounded rectangle so it sits neatly beside body text.
 * All brand backgrounds are dark enough that white foreground text satisfies
 * WCAG AA contrast; Barclays (light cyan) uses near-black text instead.
 */
@Composable
internal fun CardBrandBadge(cardName: String, modifier: Modifier = Modifier) {
    val brand = resolveCardBrand(
        cardName = cardName,
        fallbackBg = MaterialTheme.colorScheme.primary,
        fallbackText = MaterialTheme.colorScheme.onPrimary,
    )
    val fontSize = when (brand.initials.length) {
        1, 2 -> 10.sp
        3 -> 8.sp
        else -> 7.sp
    }
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(brand.bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = brand.initials,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = brand.textColor,
            letterSpacing = if (brand.initials.length > 3) 0.sp else 0.5.sp,
        )
    }
}

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

private data class CardBrandSpec(
    val initials: String,
    val bgColor: Color,
    val textColor: Color = Color.White,
)

private fun resolveCardBrand(
    cardName: String,
    fallbackBg: Color,
    fallbackText: Color,
): CardBrandSpec {
    val lower = cardName.lowercase()
    return when {
        "amex" in lower || "american express" in lower ->
            CardBrandSpec("AMEX", Color(0xFF016FD0))
        "chase" in lower ->
            CardBrandSpec("CH", Color(0xFF14274E))
        "capital one" in lower || "capitalone" in lower || "cap one" in lower ->
            CardBrandSpec("C1", Color(0xFFC8102E))
        "citi" in lower ->
            CardBrandSpec("CITI", Color(0xFF003B84))
        "discover" in lower ->
            CardBrandSpec("DISC", Color(0xFFE85D04))
        "wells fargo" in lower || "wellsfargo" in lower ->
            CardBrandSpec("WF", Color(0xFFCC0000))
        "bank of america" in lower || "bofa" in lower ->
            CardBrandSpec("BOA", Color(0xFFE21833))
        "us bank" in lower || "usbank" in lower || "u.s. bank" in lower ->
            CardBrandSpec("USB", Color(0xFF13376E))
        "barclays" in lower ->
            // Light cyan needs dark text for contrast
            CardBrandSpec("BARC", Color(0xFF00AEEF), Color(0xFF1A1A1A))
        "apple" in lower ->
            CardBrandSpec("AP", Color(0xFF1D1D1F))
        "synchrony" in lower ->
            CardBrandSpec("SY", Color(0xFF0058A3))
        "navy federal" in lower ->
            CardBrandSpec("NFCU", Color(0xFF003087))
        "amazon" in lower ->
            CardBrandSpec("AMZ", Color(0xFF232F3E))
        "goldman" in lower || "marcus" in lower ->
            CardBrandSpec("GS", Color(0xFF2B4D9C))
        else -> {
            // Derive up to 2 initials from word boundaries
            val initials = cardName
                .split(" ", "-", "/", "(")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercaseChar() }
                .joinToString("")
                .ifBlank { "?" }
            CardBrandSpec(initials, fallbackBg, fallbackText)
        }
    }
}
