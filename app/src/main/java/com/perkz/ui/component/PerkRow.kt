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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.perkz.ui.model.UiPerkItem
import com.perkz.ui.model.resolvedColors

@Composable
internal fun PerkRow(item: UiPerkItem, onCheckedChange: (Boolean) -> Unit) {
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
                Checkbox(
                    checked = item.isUsedThisPeriod,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.padding(top = 0.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.accentColor,
                        uncheckedColor = colors.accentColor.copy(alpha = 0.7f),
                        checkmarkColor = colors.cardColor,
                    )
                )
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
                        Text(
                            text = item.perk.card,
                            style = MaterialTheme.typography.bodyMedium,
                            color = onCardSecondary,
                        )
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

