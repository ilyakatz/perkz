package com.perkz.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.perkz.data.db.PerkEntity
import com.perkz.ui.component.PerkRow
import com.perkz.ui.model.UiState
import com.perkz.ui.model.PerkStatus
import com.perkz.ui.model.resolvedColors

@Composable
internal fun PerksTabContent(
    uiState: UiState,
    onCardSelect: (String) -> Unit,
    onStatusSelect: (String) -> Unit,
    onToggleStatusCollapsed: (PerkStatus) -> Unit,
    onToggleIntervalCollapsed: (String) -> Unit,
    onSetIntervalsCollapsed: (Set<String>, Boolean) -> Unit,
    onToggleUsed: (PerkEntity, Boolean) -> Unit
) {
    if (uiState.sheetUrl.isBlank()) {
        WelcomeState()
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.availableCards.isNotEmpty() || uiState.availableStatusFilters.isNotEmpty()) {
            FilterSection(
                uiState = uiState,
                onCardSelect = onCardSelect,
                onStatusSelect = onStatusSelect
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        when {
            uiState.isLoading -> LoadingState()
            !uiState.hasAnyPerks -> EmptyState()
            else -> PerkList(uiState = uiState, onToggleStatusCollapsed = onToggleStatusCollapsed, onToggleIntervalCollapsed = onToggleIntervalCollapsed, onSetIntervalsCollapsed = onSetIntervalsCollapsed, onToggleUsed = onToggleUsed)
        }
    }
}

@Composable
private fun WelcomeState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "👋 Welcome to Perkz",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Track and manage your credit card perks in one place.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Add your Google Sheet CSV URL in the Settings tab to get started.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Loading perks…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No perks found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Add your sheet URL in Settings, then tap the refresh icon to load your perks.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FilterSection(
    uiState: UiState,
    onCardSelect: (String) -> Unit,
    onStatusSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (uiState.availableCards.isNotEmpty()) {
            Text(
                text = "CARD",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.availableCards.forEach { card ->
                    FilterChip(
                        selected = card == uiState.selectedCard,
                        onClick = { onCardSelect(card) },
                        label = { Text(card) }
                    )
                }
            }
        }
        if (uiState.availableStatusFilters.isNotEmpty()) {
            Text(
                text = "STATUS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.availableStatusFilters.forEach { statusFilter ->
                    FilterChip(
                        selected = statusFilter == uiState.selectedStatusFilter,
                        onClick = { onStatusSelect(statusFilter) },
                        label = { Text(statusFilter) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PerkList(
    uiState: UiState,
    onToggleStatusCollapsed: (PerkStatus) -> Unit,
    onToggleIntervalCollapsed: (String) -> Unit,
    onSetIntervalsCollapsed: (Set<String>, Boolean) -> Unit,
    onToggleUsed: (PerkEntity, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val listScope = this
        uiState.statusGroups.forEach statusLoop@{ statusGroup ->
            val collapsed = statusGroup.status in uiState.collapsedStatuses
            item(key = "header-${statusGroup.status.name}") {
                val statusColors = statusGroup.status.resolvedColors()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleStatusCollapsed(statusGroup.status) }
                        .padding(top = 4.dp, bottom = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColors.accentColor)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = statusGroup.status.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColors.titleColor,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = if (collapsed) "+" else "-",
                        style = MaterialTheme.typography.titleMedium,
                        color = statusColors.titleColor
                    )
                }
            }
            if (!collapsed) {
                listScope.addStatusItems(statusGroup, uiState.collapsedIntervals, onToggleIntervalCollapsed, onSetIntervalsCollapsed, onToggleUsed)
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

private fun LazyListScope.addStatusItems(
    statusGroup: com.perkz.ui.model.UiStatusGroup,
    collapsedIntervals: Set<String>,
    onToggleIntervalCollapsed: (String) -> Unit,
    onSetIntervalsCollapsed: (Set<String>, Boolean) -> Unit,
    onToggleUsed: (PerkEntity, Boolean) -> Unit
) {
    val intervals = statusGroup.intervalGroups.map { it.interval }.toSet()
    if (statusGroup.intervalGroups.isEmpty()) {
        item(key = "empty-${statusGroup.status.name}") {
            Text(text = statusGroup.status.emptyText, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 18.dp, bottom = 4.dp))
        }
    } else {
        item(key = "collapse-all-${statusGroup.status.name}") {
            TextButton(
                onClick = { onSetIntervalsCollapsed(intervals, collapsedIntervals.intersect(intervals).size != intervals.size) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (collapsedIntervals.intersect(intervals).size == intervals.size) "Expand all" else "Collapse all")
            }
        }
        statusGroup.intervalGroups.forEach { intervalGroup ->
            val collapsed = intervalGroup.interval in collapsedIntervals
            item(key = "interval-${statusGroup.status.name}-${intervalGroup.interval}") {
                Row(Modifier.fillMaxWidth().clickable { onToggleIntervalCollapsed(intervalGroup.interval) },
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(text = intervalGroup.interval, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 18.dp, top = 2.dp, bottom = 2.dp))
                    Spacer(Modifier.weight(1f))
                    Text(if (collapsed) "+" else "-", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!collapsed) {
                items(intervalGroup.items, key = { it.perk.id }) { item ->
                    PerkRow(item = item, onCheckedChange = { checked -> onToggleUsed(item.perk, checked) })
                }
            }
        }
    }
}
