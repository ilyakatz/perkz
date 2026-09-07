package com.perkz.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.perkz.data.db.PerkEntity
import com.perkz.domain.formatAmount
import com.perkz.domain.isCountUnit
import com.perkz.ui.component.IntervalSubsectionHeader
import com.perkz.ui.component.PerkRow
import com.perkz.ui.component.StatusSectionHeader
import com.perkz.ui.model.PerkStatus
import com.perkz.ui.model.UiIntervalGroup
import com.perkz.ui.model.UiPerkItem
import com.perkz.ui.model.UiState
import com.perkz.ui.model.UiStatusGroup
import com.perkz.ui.model.resolvedColors
import com.perkz.ui.theme.PerkzTheme

private fun UiStatusGroup.remainingSummary(): String? {
    if (!status.showsRemainingSummary) return null
    val items = intervalGroups.flatMap { it.items }
    val remainingValues = items.mapNotNull { item ->
        item.maxAmount?.let { (it - item.usedAmount).coerceAtLeast(0.0) }
    }
    if (remainingValues.isEmpty()) return null
    val allCountUnits = items.all { isCountUnit(it.perk.maxValueOrUses) }
    val prefix = if (allCountUnits) "" else "$"
    return "$prefix${formatAmount(remainingValues.sum())}"
}

@Composable
internal fun PerkList(
    uiState: UiState,
    onToggleStatusCollapsed: (PerkStatus) -> Unit,
    onToggleIntervalCollapsed: (String) -> Unit,
    onSetIntervalsCollapsed: (Set<String>, Boolean) -> Unit,
    onToggleUsed: (PerkEntity, Boolean) -> Unit,
    onAmountAdded: (PerkEntity, Double) -> Unit,
    onMarkFull: (PerkEntity) -> Unit,
    onClearUsage: (PerkEntity) -> Unit,
    onNotApplicableChange: (PerkEntity, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        uiState.statusGroups
            .filter { it.intervalGroups.isNotEmpty() }
            .forEach { statusGroup ->
                val collapsed = statusGroup.status in uiState.collapsedStatuses
                val intervalKeys = statusGroup.intervalGroups
                    .map { "${statusGroup.status.name}|${it.interval}" }
                    .toSet()
                val allIntervalsCollapsed = uiState.collapsedIntervals
                    .intersect(intervalKeys)
                    .size == intervalKeys.size
                item(key = "header-${statusGroup.status.name}") {
                    if (collapsed) {
                        StatusSectionHeader(
                            status = statusGroup.status,
                            perkCount = statusGroup.intervalGroups.sumOf { it.items.size },
                            expanded = false,
                            onClick = { onToggleStatusCollapsed(statusGroup.status) },
                        )
                    } else {
                        val statusAccent = statusGroup.status.resolvedColors().accentColor
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(4.dp)
                                        .background(statusAccent)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    StatusSectionHeader(
                                        status = statusGroup.status,
                                        perkCount = statusGroup.intervalGroups.sumOf { it.items.size },
                                        expanded = true,
                                        remainingSummary = statusGroup.remainingSummary(),
                                        showExpandAll = statusGroup.intervalGroups.size > 1,
                                        allIntervalsCollapsed = allIntervalsCollapsed,
                                        onToggleAllIntervals = {
                                            onSetIntervalsCollapsed(
                                                intervalKeys,
                                                !allIntervalsCollapsed,
                                            )
                                        },
                                        onClick = { onToggleStatusCollapsed(statusGroup.status) },
                                    )
                                    ExpandedStatusIntervals(
                                        statusGroup = statusGroup,
                                        collapsedIntervals = uiState.collapsedIntervals,
                                        onToggleIntervalCollapsed = onToggleIntervalCollapsed,
                                        onToggleUsed = onToggleUsed,
                                        onAmountAdded = onAmountAdded,
                                        onMarkFull = onMarkFull,
                                        onClearUsage = onClearUsage,
                                        onNotApplicableChange = onNotApplicableChange,
                                    )
                                }
                            }
                        }
                    }
                }
            }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ExpandedStatusIntervals(
    statusGroup: UiStatusGroup,
    collapsedIntervals: Set<String>,
    onToggleIntervalCollapsed: (String) -> Unit,
    onToggleUsed: (PerkEntity, Boolean) -> Unit,
    onAmountAdded: (PerkEntity, Double) -> Unit,
    onMarkFull: (PerkEntity) -> Unit,
    onClearUsage: (PerkEntity) -> Unit,
    onNotApplicableChange: (PerkEntity, Boolean) -> Unit
) {
    statusGroup.intervalGroups.forEach { intervalGroup ->
        val intervalKey = "${statusGroup.status.name}|${intervalGroup.interval}"
        val collapsed = intervalKey in collapsedIntervals
        key(intervalKey) {
            IntervalSubsectionHeader(
                interval = intervalGroup.interval,
                perkCount = intervalGroup.items.size,
                collapsed = collapsed,
                onToggle = { onToggleIntervalCollapsed(intervalKey) },
            ) {
                intervalGroup.items.forEach { item ->
                    PerkRow(
                        item = item,
                        onCheckedChange = { checked -> onToggleUsed(item.perk, checked) },
                        onAmountAdded = { amount -> onAmountAdded(item.perk, amount) },
                        onMarkFull = { onMarkFull(item.perk) },
                        onClearUsage = { onClearUsage(item.perk) },
                        onNotApplicableChange = { value -> onNotApplicableChange(item.perk, value) }
                    )
                }
            }
        }
    }
}

private fun previewPerk(
    id: String,
    title: String,
    card: String,
    interval: String,
    status: PerkStatus,
    maxValueOrUses: String = "100",
    usedAmount: Double = 0.0,
): UiPerkItem {
    val maxAmount = 100.0
    return UiPerkItem(
        perk = PerkEntity(
            id = id,
            title = title,
            card = card,
            interval = interval,
            sourceRowNumber = id.hashCode(),
            resetPeriod = "End of month",
            deadlineTrigger = "",
            maxValueOrUses = maxValueOrUses,
            details = "",
            usedFromSheet = false,
            usedAmountFromSheet = null,
        ),
        isUsedThisPeriod = usedAmount >= maxAmount,
        usedAmount = usedAmount,
        maxAmount = maxAmount,
        periodLabel = "Sep 1 - 30",
        resetPeriodLabel = "Sep 30",
        status = status,
    )
}

private val previewStatusGroups = listOf(
    UiStatusGroup(
        status = PerkStatus.ExpiringSoon,
        intervalGroups = listOf(
            UiIntervalGroup(
                interval = "Monthly",
                items = listOf(
                    previewPerk("monthly-1", "Dining credit", "Amex Gold", "Monthly", PerkStatus.ExpiringSoon, usedAmount = 20.0),
                    previewPerk("monthly-2", "Ride credit", "Amex Gold", "Monthly", PerkStatus.ExpiringSoon, usedAmount = 60.0),
                ),
            ),
        ),
    ),
    UiStatusGroup(
        status = PerkStatus.NeedsUse,
        intervalGroups = listOf(
            UiIntervalGroup(
                interval = "Monthly",
                items = listOf(previewPerk("needs-use-1", "Dining credit", "Amex Platinum", "Monthly", PerkStatus.NeedsUse)),
            ),
        ),
    ),
    UiStatusGroup(
        status = PerkStatus.Upcoming,
        intervalGroups = listOf(
            UiIntervalGroup(
                interval = "Quarterly",
                items = listOf(previewPerk("upcoming-1", "Travel credit", "Chase Sapphire", "Quarterly", PerkStatus.Upcoming)),
            ),
        ),
    ),
)

private fun previewState(collapsedStatuses: Set<PerkStatus>) = UiState(
    statusGroups = previewStatusGroups,
    collapsedStatuses = collapsedStatuses,
    hasAnyPerks = true,
)

@Preview(name = "Collapsed status sections", widthDp = 390, heightDp = 500, showBackground = true)
@Composable
private fun PerkListCollapsedPreview() {
    PerkzTheme(themeMode = com.perkz.ui.model.ThemeMode.DARK) {
        PerkList(
            uiState = previewState(PerkStatus.entries.toSet()),
            onToggleStatusCollapsed = {},
            onToggleIntervalCollapsed = {},
            onSetIntervalsCollapsed = { _, _ -> },
            onToggleUsed = { _, _ -> },
            onAmountAdded = { _, _ -> },
            onMarkFull = {},
            onClearUsage = {},
            onNotApplicableChange = { _, _ -> },
        )
    }
}

@Preview(name = "Expanded interval hierarchy", widthDp = 390, heightDp = 900, showBackground = true)
@Composable
private fun PerkListExpandedPreview() {
    PerkzTheme(themeMode = com.perkz.ui.model.ThemeMode.DARK) {
        PerkList(
            uiState = previewState(PerkStatus.entries.toSet() - PerkStatus.ExpiringSoon),
            onToggleStatusCollapsed = {},
            onToggleIntervalCollapsed = {},
            onSetIntervalsCollapsed = { _, _ -> },
            onToggleUsed = { _, _ -> },
            onAmountAdded = { _, _ -> },
            onMarkFull = {},
            onClearUsage = {},
            onNotApplicableChange = { _, _ -> },
        )
    }
}
