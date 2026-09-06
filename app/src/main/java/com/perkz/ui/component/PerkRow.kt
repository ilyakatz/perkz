package com.perkz.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.perkz.ui.model.UiPerkItem
import com.perkz.ui.model.resolvedColors
import com.perkz.domain.formatAmount
import com.perkz.domain.parseAmount

@Composable
internal fun PerkRow(
    item: UiPerkItem,
    onCheckedChange: (Boolean) -> Unit,
    onAmountAdded: (Double) -> Unit,
    onNotApplicableChange: (Boolean) -> Unit
) {
    var showAmountDialog by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    val colors = item.status.resolvedColors()
    // onCard* colors are derived from the scheme so they adapt to card luminance
    val onCardPrimary = MaterialTheme.colorScheme.onSurface
    val onCardSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.cardColor,
            contentColor = onCardPrimary,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left accent bar matching status color
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(colors.accentColor)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (item.status != com.perkz.ui.model.PerkStatus.NotApplicable) {
                    Checkbox(
                        checked = item.isUsedThisPeriod,
                        onCheckedChange = { checked ->
                            if (checked) {
                                amountText = item.maxAmount?.let { max ->
                                    formatAmount((max - item.usedAmount).coerceAtLeast(0.0))
                                } ?: "1"
                                showAmountDialog = true
                            } else {
                                onCheckedChange(false)
                            }
                        },
                        modifier = Modifier.padding(top = 0.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = colors.accentColor,
                            uncheckedColor = colors.accentColor.copy(alpha = 0.7f),
                            checkmarkColor = colors.cardColor,
                        )
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Status badge pill
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = colors.accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = item.status.badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = item.perk.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = onCardPrimary,
                    )

                    if (item.perk.card.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            CardBrandBadge(cardName = item.perk.card)
                            Text(
                                text = item.perk.card,
                                style = MaterialTheme.typography.bodyMedium,
                                color = onCardSecondary,
                            )
                        }
                    }

                    // Metadata row: value + reset period
                    val hasValue = item.perk.maxValueOrUses.isNotBlank()
                    val hasReset = item.resetPeriodLabel.isNotBlank()
                    if (hasValue || hasReset) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            if (hasValue) {
                                PerkMetaItem(
                                    label = "VALUE",
                                    value = item.perk.maxValueOrUses,
                                    labelColor = onCardSecondary,
                                    valueColor = onCardPrimary,
                                )
                            }
                            if (item.maxAmount != null || item.usedAmount > 0.0) {
                                PerkMetaItem(
                                    label = "USED",
                                    value = item.maxAmount?.let {
                                        "${formatAmount(item.usedAmount)} / ${formatAmount(it)}"
                                    } ?: formatAmount(item.usedAmount),
                                    labelColor = onCardSecondary,
                                    valueColor = onCardPrimary,
                                )
                            }
                            if (hasReset) {
                                PerkMetaItem(
                                    label = "RESETS",
                                    value = item.resetPeriodLabel,
                                    labelColor = onCardSecondary,
                                    valueColor = onCardPrimary,
                                )
                            }

                            PerkMetaItem(
                                label = "PERIOD",
                                value = item.periodLabel,
                                labelColor = onCardSecondary,
                                valueColor = onCardPrimary,
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.maxAmount != null) {
                                val remaining = (item.maxAmount - item.usedAmount).coerceAtLeast(0.0)
                                Text(
                                    text = "${formatAmount(remaining)} remaining",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = onCardSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            if (item.status != com.perkz.ui.model.PerkStatus.NotApplicable) TextButton(
                                onClick = {
                                    amountText = item.maxAmount?.let { max ->
                                        formatAmount((max - item.usedAmount).coerceAtLeast(0.0))
                                    } ?: "1"
                                    showAmountDialog = true
                                }
                            ) {
                                Text(
                                    text = if (item.usedAmount > 0.0) "Add amount" else "Use amount",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (showAmountDialog) {
                            val enteredAmount = parseAmount(amountText)
                            val remaining = item.maxAmount?.minus(item.usedAmount)
                            val exceedsRemaining = remaining != null && enteredAmount != null && enteredAmount > remaining
                            AlertDialog(
                                onDismissRequest = { showAmountDialog = false },
                                title = { Text(if (item.usedAmount > 0.0) "Add perk usage" else "Use perk") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = if (item.usedAmount > 0.0) {
                                                "Used ${formatAmount(item.usedAmount)}${item.maxAmount?.let { " of ${formatAmount(it)}" } ?: ""}. Enter the additional amount."
                                            } else {
                                                "Enter the amount you used."
                                            },
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        OutlinedTextField(
                                            value = amountText,
                                            onValueChange = { amountText = it },
                                            label = { Text("Amount") },
                                            singleLine = true,
                                            isError = enteredAmount == null && amountText.isNotBlank() || exceedsRemaining
                                        )
                                        if (exceedsRemaining) {
                                            Text(
                                                text = "Only ${formatAmount(remaining!!)} remaining.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        enabled = enteredAmount != null && enteredAmount > 0.0 && !exceedsRemaining,
                                        onClick = {
                                            onAmountAdded(enteredAmount!!)
                                            amountText = ""
                                            showAmountDialog = false
                                        }
                                    ) { Text("Save") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showAmountDialog = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }

                    if (item.perk.deadlineTrigger.isNotBlank()) {
                        Text(
                            text = "⚠ Deadline: ${item.perk.deadlineTrigger}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.accentColor,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (item.perk.details.isNotBlank()) {
                        Text(
                            text = item.perk.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = onCardSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    TextButton(
                        onClick = {
                            onNotApplicableChange(item.status != com.perkz.ui.model.PerkStatus.NotApplicable)
                        },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = if (item.status == com.perkz.ui.model.PerkStatus.NotApplicable) {
                                "Restore perk"
                            } else {
                                "Not applicable"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PerkMetaItem(
    label: String,
    value: String,
    labelColor: androidx.compose.ui.graphics.Color,
    valueColor: androidx.compose.ui.graphics.Color,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = valueColor,
        )
    }
}
