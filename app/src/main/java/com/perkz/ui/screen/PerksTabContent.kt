package com.perkz.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.perkz.data.db.PerkEntity
import com.perkz.ui.component.PerkRow
import com.perkz.ui.model.UiState

@Composable
internal fun PerksTabContent(
    uiState: UiState,
    onCardSelect: (String) -> Unit,
    onStatusSelect: (String) -> Unit,
    onToggleUsed: (PerkEntity, Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    if (uiState.sheetUrl.isBlank()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Welcome to Perkz!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "To get started, please add your Google Sheet CSV URL in the Settings tab.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = onRefresh) {
            Text("↻ Refresh")
        }

        if (uiState.availableCards.isNotEmpty()) {
            Text(
                text = "Filter by card",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
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
                text = "Filter by status",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
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

        if (uiState.isLoading) {
            CircularProgressIndicator()
        }

        if (!uiState.hasAnyPerks && !uiState.isLoading) {
            Text("No perks found yet. Add your sheet URL in Settings, then tap Refresh.")
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.statusGroups.forEach { statusGroup ->
                item(key = "status-${statusGroup.status.name}") {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = statusGroup.status.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusGroup.status.titleColor
                    )
                }
                if (statusGroup.intervalGroups.isEmpty()) {
                    item(key = "empty-${statusGroup.status.name}") {
                        Text(
                            text = statusGroup.status.emptyText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    statusGroup.intervalGroups.forEach { intervalGroup ->
                        item(key = "interval-${statusGroup.status.name}-${intervalGroup.interval}") {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = intervalGroup.interval,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        items(items = intervalGroup.items, key = { it.perk.id }) { item ->
                            PerkRow(
                                item = item,
                                onCheckedChange = { checked ->
                                    onToggleUsed(item.perk, checked)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
