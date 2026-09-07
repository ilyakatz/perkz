package com.perkz.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.perkz.domain.formatAmount
import com.perkz.domain.formatUnitAmount
import com.perkz.ui.model.PerkStatus
import com.perkz.ui.model.UiPerkItem
import com.perkz.ui.model.resolvedColors

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
    var amountText by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
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
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header: brand badge + title/card, status badge + overflow menu
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                CardBrandBadge(cardName = item.perk.card, size = 30.dp)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.perk.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = onCardPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.perk.card.isNotBlank()) {
                        Text(
                            text = item.perk.card,
                            style = MaterialTheme.typography.bodySmall,
                            color = onCardSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = colors.accentColor.copy(alpha = 0.14f),
                    modifier = Modifier.heightIn(min = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = badgeIconFor(item.status),
                            contentDescription = null,
                            tint = colors.accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = statusBadgeText(item.status),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.accentColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Box {
                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier
                            .size(32.dp)
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

            // Meta line: value/benefit, current period range, reset cadence
            val metaParts = buildList {
                if (item.perk.maxValueOrUses.isNotBlank()) {
                    val benefitValue = item.maxAmount?.let { formatUnitAmount(item.perk.maxValueOrUses, it) }
                        ?: item.perk.maxValueOrUses
                    add("$benefitValue ${item.perk.interval.trim().ifBlank { "Monthly" }.lowercase()} benefit")
                }
                if (item.periodRangeLabel.isNotBlank()) add(item.periodRangeLabel)
                if (item.resetPeriodLabel.isNotBlank()) {
                    add("Resets ${item.perk.interval.trim().ifBlank { "Monthly" }.lowercase()}")
                }
            }
            if (metaParts.isNotEmpty()) {
                Text(
                    text = metaParts.joinToString("  |  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = onCardSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Details / usage instructions link line
            if (item.perk.details.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = item.perk.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (item.perk.deadlineTrigger.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = colors.accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Deadline: ${item.perk.deadlineTrigger}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.accentColor,
                    )
                }
            }

            if (!isNotApplicable) {
                val remaining = item.maxAmount?.let { (it - item.usedAmount).coerceAtLeast(0.0) }
                if (item.maxAmount != null) {
                    UsageRing(
                        usedAmount = item.usedAmount,
                        maxAmount = item.maxAmount,
                        remaining = remaining!!,
                        rawUnit = item.perk.maxValueOrUses,
                        accentColor = colors.accentColor,
                        primaryTextColor = onCardPrimary,
                        secondaryTextColor = onCardSecondary,
                    )
                } else {
                    Text(
                        text = "Usage tracked",
                        style = MaterialTheme.typography.bodySmall,
                        color = onCardSecondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            amountText = remaining?.let(::formatAmount) ?: "1"
                            showAmountDialog = true
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Add amount",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onMarkFull,
                        enabled = item.maxAmount != null && remaining != null && remaining > 0.0,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Mark full",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (showAmountDialog) {
                    RecordUsageDialog(
                        amountText = amountText,
                        maxValueOrUses = item.perk.maxValueOrUses,
                        maxAmount = item.maxAmount,
                        usedAmount = item.usedAmount,
                        onAmountTextChange = { amountText = it },
                        onDismiss = { showAmountDialog = false },
                        onAmountAdded = {
                            onAmountAdded(it)
                            amountText = ""
                            showAmountDialog = false
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

@Composable
private fun UsageRing(
    usedAmount: Double,
    maxAmount: Double,
    remaining: Double,
    rawUnit: String,
    accentColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
) {
    val progress = (usedAmount / maxAmount).coerceIn(0.0, 1.0).toFloat()
    val percent = (progress * 100).toInt()
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(88.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(88.dp),
                color = accentColor.copy(alpha = 0.15f),
                strokeWidth = 8.dp,
                trackColor = Color.Transparent,
            )
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(88.dp),
                color = accentColor,
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
                .padding(vertical = 2.dp)
        ) {
            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .height(64.dp)
                    .background(secondaryTextColor.copy(alpha = 0.25f))
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "${formatUnitAmount(rawUnit, usedAmount)} / ${formatUnitAmount(rawUnit, maxAmount)} used",
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor,
            )
            Text(
                text = formatUnitAmount(rawUnit, remaining),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
            )
            Text(
                text = "remaining",
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun badgeIconFor(status: PerkStatus): ImageVector = when (status) {
    PerkStatus.ExpiringSoon, PerkStatus.Expired -> Icons.Filled.Schedule
    PerkStatus.NeedsUse -> Icons.Filled.PriorityHigh
    PerkStatus.Upcoming -> Icons.Filled.Event
    PerkStatus.PartiallyUsed -> Icons.AutoMirrored.Filled.TrendingUp
    PerkStatus.Used -> Icons.Filled.CheckCircle
    PerkStatus.NotApplicable -> Icons.Filled.Block
}
