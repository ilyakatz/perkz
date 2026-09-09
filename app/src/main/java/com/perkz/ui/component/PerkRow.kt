@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.perkz.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.perkz.data.db.PerkEntity
import com.perkz.domain.formatAmount
import com.perkz.domain.formatUnitAmount
import com.perkz.domain.isCountUnit
import com.perkz.domain.BenefitUnit
import com.perkz.domain.resolvedBenefitUnit
import com.perkz.domain.isQuantity
import com.perkz.domain.quantityLabel
import com.perkz.ui.model.PerkStatus
import com.perkz.ui.model.ThemeMode
import com.perkz.ui.model.UiPerkItem
import com.perkz.ui.model.resolvedColors
import com.perkz.ui.theme.PerkzTheme

@Composable
internal fun PerkRow(
    item: UiPerkItem,
    onCheckedChange: (Boolean) -> Unit,
    onAmountAdded: (Double) -> Unit,
    onMarkFull: () -> Unit,
    onClearUsage: () -> Unit,
    onNotApplicableChange: (Boolean) -> Unit
) {
    var showAmountDialog by remember { mutableStateOf(false) }
    var isSubtracting by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var detailsExpanded by remember { mutableStateOf(false) }
    var detailsOverflowing by remember { mutableStateOf(false) }
    val colors = item.status.resolvedColors()
    // onCard* colors are derived from the scheme so they adapt to card luminance
    val onCardPrimary = MaterialTheme.colorScheme.onSurface
    val onCardSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val isNotApplicable = item.status == PerkStatus.NotApplicable

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = onCardPrimary,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: brand badge + title/card + overflow menu. The status badge lives on
            // its own row below so it never squeezes a long, two-line title.
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                CardBrandBadge(cardName = item.perk.card, size = 44.dp, cornerRadius = 10.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.perk.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onCardPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.perk.card.isNotBlank()) {
                        Text(
                            text = item.perk.card,
                            style = MaterialTheme.typography.bodySmall,
                            color = onCardSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Box {
                    // Fixed 48dp touch target with a muted circular surface, per the
                    // reference's circular overflow affordance.
                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .semantics { contentDescription = "More actions for ${item.perk.title}" }
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null, tint = onCardSecondary)
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        if (item.usedAmount > 0.0) {
                            DropdownMenuItem(
                                text = { Text("Subtract usage") },
                                onClick = {
                                    showMoreMenu = false
                                    isSubtracting = true
                                    amountText = ""
                                    showAmountDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear usage") },
                                onClick = {
                                    showMoreMenu = false
                                    showClearConfirmation = true
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (isNotApplicable) "Restore perk" else "Mark not applicable") },
                            onClick = {
                                showMoreMenu = false
                                onNotApplicableChange(!isNotApplicable)
                            }
                        )
                    }
                }
            }

            // Status badge: its own row so it wraps freely under a long title instead of
            // fighting it for horizontal space. A small solid dot (not an icon) leads the
            // label, matching the reference pill.
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = colors.accentColor.copy(alpha = 0.14f),
                modifier = Modifier.heightIn(min = 28.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.accentColor)
                    )
                    Text(
                        text = statusBadgeText(item.status),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.accentColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Metadata: icon-led rows instead of a single clipped joined string. First row
            // pairs the interval benefit with the current period range behind a vertical
            // divider; second row shows the reset cadence. Both wrap freely on narrow
            // widths / large font scale via FlowRow instead of clipping.
            val benefitLabel = "${item.perk.interval.trim().ifBlank { "Monthly" }} benefit"
            MetaFlowRow(modifier = Modifier.padding(top = 4.dp)) {
                MetaIconLabel(
                    icon = Icons.Filled.LocalOffer,
                    text = benefitLabel,
                    color = onCardSecondary,
                )
                if (item.periodRangeLabel.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MetaVerticalDivider(color = onCardSecondary)
                        Spacer(Modifier.width(10.dp))
                        MetaIconLabel(
                            icon = Icons.Filled.CalendarMonth,
                            text = item.periodRangeLabel,
                            color = onCardSecondary,
                        )
                    }
                }
            }
            if (item.resetPeriodLabel.isNotBlank()) {
                MetaIconLabel(
                    icon = Icons.Filled.Autorenew,
                    text = "Reset: ${item.resetPeriodLabel}",
                    color = onCardSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            // Details / usage instructions link line, capped at 3 lines with an
            // accessible Show more/Show less toggle when the text overflows.
            if (item.perk.details.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.perk.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = if (detailsExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { layoutResult ->
                                if (!detailsExpanded) {
                                    detailsOverflowing = layoutResult.hasVisualOverflow
                                }
                            },
                        )
                        if (detailsOverflowing || detailsExpanded) {
                            TextButton(
                                onClick = { detailsExpanded = !detailsExpanded },
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .semantics {
                                        contentDescription = if (detailsExpanded) {
                                            "Show less details for ${item.perk.title}"
                                        } else {
                                            "Show more details for ${item.perk.title}"
                                        }
                                    }
                            ) {
                                Text(
                                    text = if (detailsExpanded) "Show less" else "Show more",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            if (item.perk.deadlineTrigger.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = colors.accentColor,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                    )
                    Text(
                        text = "Deadline: ${item.perk.deadlineTrigger}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.accentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!isNotApplicable) {
                val remaining = item.maxAmount?.let { (it - item.usedAmount).coerceAtLeast(0.0) }
                // Subtle divider separates details/deadline copy from the usage summary.
                HorizontalDivider(
                    modifier = Modifier.padding(top = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                if (item.maxAmount != null) {
                    UsageSummary(
                        usedAmount = item.usedAmount,
                        maxAmount = item.maxAmount,
                        remaining = remaining!!,
                        perk = item.perk,
                        primaryTextColor = onCardPrimary,
                        secondaryTextColor = onCardSecondary,
                    )
                } else {
                    Text(
                        text = "Usage tracked",
                        style = MaterialTheme.typography.bodySmall,
                        color = onCardSecondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                PerkActionButtons(
                    onAddAmountClick = {
                        amountText = remaining?.let(::formatAmount) ?: "1"
                        showAmountDialog = true
                    },
                    onMarkFull = onMarkFull,
                    markFullEnabled = item.maxAmount != null && remaining != null && remaining > 0.0,
                    modifier = Modifier.padding(top = 12.dp)
                )

                if (showAmountDialog) {
                    RecordUsageDialog(
                        amountText = amountText,
                        unit = item.perk.resolvedBenefitUnit(),
                        maxValueOrUses = item.perk.maxValueOrUses,
                        maxAmount = item.maxAmount,
                        usedAmount = item.usedAmount,
                        isSubtraction = isSubtracting,
                        onAmountTextChange = { amountText = it },
                        onDismiss = {
                            showAmountDialog = false
                            isSubtracting = false
                        },
                        onAmountAdded = {
                            onAmountAdded(it)
                            amountText = ""
                            showAmountDialog = false
                            isSubtracting = false
                        },
                    )
                }
            } else {
                Text(
                    text = "Marked not applicable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = onCardSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (showClearConfirmation) {
                AlertDialog(
                    onDismissRequest = { showClearConfirmation = false },
                    title = { Text("Clear usage?") },
                    text = { Text("This clears the used amount and date for the current period.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showClearConfirmation = false
                            onClearUsage()
                        }) { Text("Clear") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirmation = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

private fun statusBadgeText(status: PerkStatus): String {
    if (status != PerkStatus.ExpiringSoon) return status.badgeText
    val today = LocalDate.now()
    val daysLeft = ChronoUnit.DAYS.between(today, today.withDayOfMonth(today.lengthOfMonth()))
    return "${daysLeft.coerceAtLeast(0)} days left"
}

/**
 * Lays out the "Record usage" / "Mark full" actions side by side when both fit at their
 * natural, single-line label width, and stacks them vertically otherwise (e.g. narrow
 * screens or large font scale) so labels never wrap, squash, or rotate.
 */
@Composable
private fun PerkActionButtons(
    onAddAmountClick: () -> Unit,
    onMarkFull: () -> Unit,
    markFullEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = 10.dp
    SubcomposeLayout(modifier = modifier.fillMaxWidth()) { constraints ->
        val spacingPx = spacing.roundToPx()
        val unbounded = Constraints()

        val addNaturalWidth = subcompose("actionsProbeAdd") {
            RecordUsageButtonContent(onClick = {})
        }.first().measure(unbounded).width

        val markNaturalWidth = subcompose("actionsProbeMark") {
            MarkFullButtonContent(onClick = {}, enabled = markFullEnabled)
        }.first().measure(unbounded).width

        // Both buttons get an equal share of the width when placed side by side, so
        // side-by-side only fits if double the wider button's natural width still fits.
        val fitsSideBySide = (maxOf(addNaturalWidth, markNaturalWidth) * 2 + spacingPx) <= constraints.maxWidth

        val contentPlaceable = subcompose("actionsContent") {
            if (fitsSideBySide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    RecordUsageButtonContent(
                        onClick = onAddAmountClick,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    )
                    MarkFullButtonContent(
                        onClick = onMarkFull,
                        enabled = markFullEnabled,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    RecordUsageButtonContent(
                        onClick = onAddAmountClick,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    )
                    MarkFullButtonContent(
                        onClick = onMarkFull,
                        enabled = markFullEnabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    )
                }
            }
        }.first().measure(constraints)

        layout(contentPlaceable.width, contentPlaceable.height) {
            contentPlaceable.placeRelative(0, 0)
        }
    }
}

@Composable
private fun RecordUsageButtonContent(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            "Record usage",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MarkFullButtonContent(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.38f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            "Mark full",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun UsageSummary(
    usedAmount: Double,
    maxAmount: Double,
    remaining: Double,
    perk: PerkEntity,
    primaryTextColor: Color,
    secondaryTextColor: Color,
) {
    val unit = perk.resolvedBenefitUnit()
    val rawUnit = perk.maxValueOrUses
    val isCountBased = if (unit == BenefitUnit.AUTO) isCountUnit(rawUnit) else unit.isQuantity()
    val label = unit.quantityLabel() ?: "uses"

    val usedText = if (isCountBased) {
        "${formatUnitAmount(unit, rawUnit, usedAmount)} of ${formatUnitAmount(unit, rawUnit, maxAmount)} $label"
    } else {
        "${formatUnitAmount(unit, rawUnit, usedAmount)} / ${formatUnitAmount(unit, rawUnit, maxAmount)} used"
    }
    val remainingText = formatUnitAmount(unit, rawUnit, remaining)
    val progress = (usedAmount / maxAmount.coerceAtLeast(1.0)).coerceIn(0.0, 1.0).toFloat()
    val percent = (progress * 100).toInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$usedText, $remainingText remaining"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(88.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(88.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                strokeWidth = 8.dp,
                trackColor = Color.Transparent,
            )
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(88.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 8.dp,
                trackColor = Color.Transparent,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryTextColor,
                )
                Text(
                    text = "used",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryTextColor,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(64.dp)
                .background(secondaryTextColor.copy(alpha = 0.25f))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = usedText,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor,
            )
            Text(
                text = remainingText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "remaining",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Icon-led metadata label, e.g. a benefit, period, or reset row. */
@Composable
private fun MetaIconLabel(
    icon: ImageVector,
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

/** Thin vertical divider used between metadata/usage segments. */
@Composable
private fun MetaVerticalDivider(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(1.dp)
            .height(14.dp)
            .background(color.copy(alpha = 0.4f))
    )
}

/**
 * Wraps metadata segments (icon-led label rows) onto multiple lines instead of clipping
 * when they don't fit on narrow widths or at large font scale.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetaFlowRow(
    modifier: Modifier = Modifier,
    content: @Composable FlowRowScope.() -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

private fun previewItem(
    title: String,
    card: String,
    details: String,
    deadlineTrigger: String,
    maxValueOrUses: String,
    usedAmount: Double,
    maxAmount: Double?,
    interval: String = "Monthly",
    resetPeriod: String = "End of month",
    periodRangeLabel: String = "Sep 1 - 30",
): UiPerkItem = UiPerkItem(
    perk = PerkEntity(
        id = title,
        title = title,
        card = card,
        interval = interval,
        sourceRowNumber = title.hashCode(),
        resetPeriod = resetPeriod,
        deadlineTrigger = deadlineTrigger,
        maxValueOrUses = maxValueOrUses,
        details = details,
        benefitUnit = "auto",
        usedFromSheet = false,
        usedAmountFromSheet = null,
    ),
    isUsedThisPeriod = maxAmount != null && usedAmount >= maxAmount,
    usedAmount = usedAmount,
    maxAmount = maxAmount,
    periodLabel = periodRangeLabel,
    resetPeriodLabel = resetPeriod,
    status = PerkStatus.NeedsUse,
    periodRangeLabel = periodRangeLabel,
)

private val shortPreviewItem = previewItem(
    title = "Dining credit",
    card = "Amex Gold",
    details = "",
    deadlineTrigger = "",
    maxValueOrUses = "$10",
    usedAmount = 0.0,
    maxAmount = 10.0,
)

// Mirrors the reference screenshot: a one-time, count-based enrollment perk with a
// two-line title, wrapping metadata, long details/deadline copy, and "0 of 1 uses".
private val longPreviewItem = previewItem(
    title = "Hertz Five Star status enrollment",
    card = "Amex Gold (2026)",
    details = "Requires Hertz Gold Plus account and matching email; allow up to 72 hours for " +
        "status to appear; recheck after card replacement.",
    deadlineTrigger = "Enroll through Amex before use.",
    maxValueOrUses = "1 use",
    usedAmount = 0.0,
    maxAmount = 1.0,
    interval = "One-time",
    resetPeriod = "None",
    periodRangeLabel = "Sep 1 - 30",
)

// Preserves currency-based long-content coverage (value + partial usage) alongside the
// count-based reference example above.
private val longCurrencyPreviewItem = previewItem(
    title = "Hertz Five Star status enrollment and complimentary upgrade benefit",
    card = "Amex Gold Card (Personal, opened 2026)",
    details = "Requires Hertz Gold Plus Rewards account and a matching email address on file; " +
        "allow up to 72 hours for status to appear after enrollment; recheck after any card " +
        "replacement or reissue, since enrollment is tied to the physical card number.",
    deadlineTrigger = "Enroll through the Amex offer page before your first rental to receive credit.",
    maxValueOrUses = "$300",
    usedAmount = 45.0,
    maxAmount = 300.0,
)


@Preview(name = "Short content - Light - 320dp", widthDp = 320, showBackground = true)
@Preview(name = "Short content - Light - 360dp", widthDp = 360, showBackground = true)
@Preview(name = "Short content - Light - 390dp", widthDp = 390, showBackground = true)
@Composable
private fun PerkRowShortContentLightPreview() {
    PerkzTheme(themeMode = ThemeMode.LIGHT) {
        PerkRow(
            item = shortPreviewItem,
            onCheckedChange = {},
            onAmountAdded = {},
            onMarkFull = {},
            onClearUsage = {},
            onNotApplicableChange = {},
        )
    }
}

@Preview(
    name = "Short content - Dark - 320dp",
    widthDp = 320,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(
    name = "Short content - Dark - 360dp",
    widthDp = 360,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(
    name = "Short content - Dark - 390dp",
    widthDp = 390,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PerkRowShortContentDarkPreview() {
    PerkzTheme(themeMode = ThemeMode.DARK) {
        PerkRow(
            item = shortPreviewItem,
            onCheckedChange = {},
            onAmountAdded = {},
            onMarkFull = {},
            onClearUsage = {},
            onNotApplicableChange = {},
        )
    }
}

@Preview(name = "Long content - Light - 320dp", widthDp = 320, showBackground = true)
@Preview(name = "Long content - Light - 360dp", widthDp = 360, showBackground = true)
@Preview(name = "Long content - Light - 390dp", widthDp = 390, showBackground = true)
@Composable
private fun PerkRowLongContentLightPreview() {
    PerkzTheme(themeMode = ThemeMode.LIGHT) {
        PerkRow(
            item = longPreviewItem,
            onCheckedChange = {},
            onAmountAdded = {},
            onMarkFull = {},
            onClearUsage = {},
            onNotApplicableChange = {},
        )
    }
}

@Preview(
    name = "Long content - Dark - 320dp",
    widthDp = 320,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(
    name = "Long content - Dark - 360dp",
    widthDp = 360,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(
    name = "Long content - Dark - 390dp",
    widthDp = 390,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PerkRowLongContentDarkPreview() {
    PerkzTheme(themeMode = ThemeMode.DARK) {
        PerkRow(
            item = longPreviewItem,
            onCheckedChange = {},
            onAmountAdded = {},
            onMarkFull = {},
            onClearUsage = {},
            onNotApplicableChange = {},
        )
    }
}

@Preview(
    name = "Long content - Light - 360dp @2x font scale",
    widthDp = 360,
    fontScale = 2f,
    showBackground = true,
)
@Composable
private fun PerkRowLongContentLargeFontPreview() {
    PerkzTheme(themeMode = ThemeMode.LIGHT) {
        PerkRow(
            item = longPreviewItem,
            onCheckedChange = {},
            onAmountAdded = {},
            onMarkFull = {},
            onClearUsage = {},
            onNotApplicableChange = {},
        )
    }
}

// Currency-based long-content coverage (value + partial usage), distinct from the
// count-based reference example above.
@Preview(name = "Long content currency - Light - 360dp", widthDp = 360, showBackground = true)
@Preview(
    name = "Long content currency - Dark - 360dp",
    widthDp = 360,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(
    name = "Long content currency - Light - 360dp @2x font scale",
    widthDp = 360,
    fontScale = 2f,
    showBackground = true,
)
@Composable
private fun PerkRowLongContentCurrencyPreview() {
    PerkzTheme {
        PerkRow(
            item = longCurrencyPreviewItem,
            onCheckedChange = {},
            onAmountAdded = {},
            onMarkFull = {},
            onClearUsage = {},
            onNotApplicableChange = {},
        )
    }
}
