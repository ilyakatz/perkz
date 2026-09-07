package com.perkz.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.perkz.data.db.PerkEntity
import com.perkz.domain.cardLabelForFilter
import com.perkz.domain.formatAmount
import com.perkz.domain.isCountUnit
import com.perkz.ui.component.PerkRow
import com.perkz.ui.model.ALL_CARDS_FILTER
import com.perkz.ui.model.UiState
import com.perkz.ui.model.PerkStatus
import com.perkz.ui.model.ALL_STATUSES_FILTER
import com.perkz.ui.model.ATTENTION_FILTER
import com.perkz.ui.model.DEFAULT_STATUS_FILTERS
import com.perkz.ui.model.applyPerkFilters
import com.perkz.ui.model.isAllStatusesSelection
import com.perkz.ui.model.normalizeStatusFilters
import com.perkz.ui.model.resolvedColors
import com.perkz.ui.model.statusPresetLabelFor

@Composable
internal fun PerksTabContent(
    uiState: UiState,
    onCardSelect: (String) -> Unit,
    onStatusPresetSelect: (Set<PerkStatus>) -> Unit,
    onApplyFilters: (Set<String>, Set<PerkStatus>) -> Unit,
    onClearFilters: () -> Unit,
    onToggleStatusCollapsed: (PerkStatus) -> Unit,
    onToggleIntervalCollapsed: (String) -> Unit,
    onSetIntervalsCollapsed: (Set<String>, Boolean) -> Unit,
    onToggleUsed: (PerkEntity, Boolean) -> Unit,
    onAmountAdded: (PerkEntity, Double) -> Unit,
    onMarkFull: (PerkEntity) -> Unit,
    onClearUsage: (PerkEntity) -> Unit,
    onNotApplicableChange: (PerkEntity, Boolean) -> Unit
) {
    if (uiState.sheetUrl.isBlank()) {
        WelcomeState()
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.availableCards.isNotEmpty() || uiState.statusCounts.isNotEmpty()) {
            FilterSection(
                uiState = uiState,
                onCardSelect = onCardSelect,
                onStatusPresetSelect = onStatusPresetSelect,
                onApplyFilters = onApplyFilters,
                onClearFilters = onClearFilters
            )
        }

        when {
            uiState.isLoading -> LoadingState()
            !uiState.hasAnyPerks -> EmptyState()
            else -> PerkList(
                uiState = uiState,
                onToggleStatusCollapsed = onToggleStatusCollapsed,
                onToggleIntervalCollapsed = onToggleIntervalCollapsed,
                onSetIntervalsCollapsed = onSetIntervalsCollapsed,
                onToggleUsed = onToggleUsed,
                onAmountAdded = onAmountAdded,
                onMarkFull = onMarkFull,
                onClearUsage = onClearUsage,
                onNotApplicableChange = onNotApplicableChange
            )
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

private data class StatusPresetOption(
    val label: String,
    val statuses: Set<PerkStatus>,
    val status: PerkStatus? = null,
    val supportingText: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSection(
    uiState: UiState,
    onCardSelect: (String) -> Unit,
    onStatusPresetSelect: (Set<PerkStatus>) -> Unit,
    onApplyFilters: (Set<String>, Set<PerkStatus>) -> Unit,
    onClearFilters: () -> Unit
) {
    var cardMenuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val selectedCardOption = uiState.selectedCards.singleOrNull()
        ?: if (uiState.selectedCards.isEmpty()) ALL_CARDS_FILTER else null
    val selectedCardLabel = when {
        uiState.selectedCards.isEmpty() -> ALL_CARDS_FILTER
        uiState.selectedCards.size == 1 -> uiState.selectedCards.first()
        else -> "${uiState.selectedCards.size} cards"
    }
    val selectedStatuses = normalizeStatusFilters(uiState.selectedStatuses)
    val selectedStatusLabel = statusPresetLabelFor(selectedStatuses)
    val appliedStatusFilters = if (
        selectedStatuses == DEFAULT_STATUS_FILTERS || selectedStatuses.isAllStatusesSelection()
    ) emptyList() else PerkStatus.entries.filter { it in selectedStatuses }
    val appliedCardFilters = uiState.selectedCards.toList().sorted()
    val appliedFilterCount = appliedStatusFilters.size + appliedCardFilters.size
    val viewportWidth = LocalConfiguration.current.screenWidthDp
    val horizontalGap = if (viewportWidth <= 320) 8.dp else 12.dp
    val fixedWideFilterWidths = viewportWidth >= 390
    val statusPresetOptions = buildList {
        add(
            StatusPresetOption(
                label = ATTENTION_FILTER,
                statuses = DEFAULT_STATUS_FILTERS,
                supportingText = "Expiring soon + Needs use"
            )
        )
        add(StatusPresetOption(ALL_STATUSES_FILTER, emptySet()))
        PerkStatus.entries.forEach { add(StatusPresetOption(it.label, setOf(it), it)) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(horizontalGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .then(
                        if (fixedWideFilterWidths) Modifier.width(132.dp)
                        else Modifier.weight(1f)
                    )
                    .height(48.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Select card filter"
                    }
                    .clickable(
                        role = Role.Button,
                        onClick = { cardMenuExpanded = true }
                    ),
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .align(Alignment.Center),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.CreditCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            selectedCardLabel.ifBlank { ALL_CARDS_FILTER },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            DropdownMenu(
                expanded = cardMenuExpanded,
                onDismissRequest = { cardMenuExpanded = false },
                offset = DpOffset(0.dp, 8.dp),
                modifier = Modifier
                    .widthIn(min = 240.dp)
                    .heightIn(max = 320.dp)
            ) {
                (listOf(ALL_CARDS_FILTER) + uiState.availableCards).forEach { card ->
                    val selected = card == selectedCardOption
                    val count = if (card == ALL_CARDS_FILTER) {
                        uiState.allItems.size
                    } else {
                        uiState.cardCounts[card].orZero()
                    }
                    DropdownMenuItem(
                        onClick = {
                            cardMenuExpanded = false
                            onCardSelect(card)
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(selected = selected, onClick = null)
                                Text(card, modifier = Modifier.weight(1f))
                                CountBadge(count = count)
                                if (selected) {
                                    Icon(Icons.Filled.Check, contentDescription = null)
                                }
                            }
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .then(
                        if (fixedWideFilterWidths) Modifier.width(148.dp)
                        else Modifier.weight(1f)
                    )
                    .height(48.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Select status filter"
                    }
                    .clickable(
                        role = Role.Button,
                        onClick = { statusMenuExpanded = true }
                    ),
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .align(Alignment.Center),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(statusIndicatorColor(selectedStatuses))
                        )
                        Text(
                            selectedStatusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        CountBadge(count = statusPreviewCount(uiState, selectedStatuses))
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            DropdownMenu(
                expanded = statusMenuExpanded,
                onDismissRequest = { statusMenuExpanded = false },
                offset = DpOffset(0.dp, 8.dp),
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 280.dp)
                    .heightIn(max = 384.dp)
            ) {
                statusPresetOptions.forEach { option ->
                    val selected = normalizeStatusFilters(option.statuses) == selectedStatuses
                    DropdownMenuItem(
                        onClick = {
                            statusMenuExpanded = false
                            onStatusPresetSelect(option.statuses)
                        },
                        modifier = Modifier.semantics {
                            if (option.supportingText != null) {
                                contentDescription = "${option.label}: ${option.supportingText}"
                            }
                        },
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(statusDotColor(option))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        option.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    option.supportingText?.let {
                                        Text(
                                            it,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                CountBadge(count = statusOptionCount(uiState, option))
                                if (selected) {
                                    Icon(Icons.Filled.Check, contentDescription = null)
                                }
                            }
                        }
                    )
                }
            }

            BadgedBox(
                badge = {
                    if (appliedFilterCount > 0) {
                        Badge { Text(appliedFilterCount.toString()) }
                    }
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(role = Role.Button, onClick = { showFilterSheet = true })
                        .semantics { contentDescription = "Open detailed filters" },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        if (appliedFilterCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    appliedCardFilters.forEach { card ->
                        InputChip(
                            selected = true,
                            onClick = { onApplyFilters(uiState.selectedCards - card, uiState.selectedStatuses) },
                            label = {
                                Text(
                                    card,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = { Icon(Icons.Filled.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier
                                .height(40.dp)
                                .semantics { contentDescription = "Remove $card filter" }
                        )
                    }
                    appliedStatusFilters.forEach { status ->
                        InputChip(
                            selected = true,
                            onClick = { onApplyFilters(uiState.selectedCards, uiState.selectedStatuses - status) },
                            label = {
                                Text(
                                    status.label,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Box(
                                    Modifier.size(12.dp).clip(CircleShape).background(status.resolvedColors().accentColor)
                                )
                            },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier
                                .height(40.dp)
                                .semantics { contentDescription = "Remove ${status.label} filter" }
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = onClearFilters,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Clear all")
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            uiState = uiState,
            onDismiss = { showFilterSheet = false },
            onApply = { cards, statuses ->
                onApplyFilters(cards, statuses)
                showFilterSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    uiState: UiState,
    onDismiss: () -> Unit,
    onApply: (Set<String>, Set<PerkStatus>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCards by remember(uiState.selectedCards, uiState.availableCards) {
        mutableStateOf(uiState.selectedCards.ifEmpty { uiState.availableCards.toSet() })
    }
    var selectedStatuses by remember(uiState.selectedStatuses) {
        mutableStateOf(uiState.selectedStatuses.ifEmpty { PerkStatus.entries.toSet() })
    }
    val previewCount = applyPerkFilters(
        items = uiState.allItems,
        selectedCards = selectedCards,
        selectedStatuses = selectedStatuses,
        cardSelector = { cardLabelForFilter(it.perk.card) },
        statusSelector = { it.status }
    ).size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filter perks", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "Close filters" }
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Cards", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                uiState.availableCards.forEach { card ->
                    val checked = card in selectedCards
                    FilterToggleRow(
                        checked = checked,
                        label = card,
                        count = uiState.cardCounts[card].orZero(),
                        onToggle = {
                            selectedCards = if (checked) selectedCards - card else selectedCards + card
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Statuses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                PerkStatus.entries.forEach { status ->
                    val checked = status in selectedStatuses
                    FilterToggleRow(
                        checked = checked,
                        label = status.label,
                        count = uiState.statusCounts[status].orZero(),
                        leadingDotColor = status.resolvedColors().accentColor,
                        modifier = Modifier.fillMaxWidth(),
                        onToggle = {
                            selectedStatuses = if (checked) selectedStatuses - status else selectedStatuses + status
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        selectedCards = emptySet()
                        selectedStatuses = emptySet()
                    },
                    modifier = Modifier
                        .widthIn(min = 96.dp)
                        .height(48.dp)
                ) {
                    Text("Clear all")
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { onApply(selectedCards, selectedStatuses) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Show $previewCount perks")
                }
            }
        }
    }
}

@Composable
private fun FilterToggleRow(
    checked: Boolean,
    label: String,
    count: Int,
    leadingDotColor: Color? = null,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable { onToggle() }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        if (leadingDotColor != null) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(leadingDotColor))
        }
        Text(
            label,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        CountBadge(count = count)
    }
}

@Composable
private fun CountBadge(count: Int, containerColor: Color = MaterialTheme.colorScheme.surfaceVariant) {
    Text(
        text = count.toString(),
        modifier = Modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )
}

private fun attentionCount(uiState: UiState): Int =
    uiState.statusCounts[PerkStatus.ExpiringSoon].orZero() +
        uiState.statusCounts[PerkStatus.NeedsUse].orZero()

private fun Int?.orZero(): Int = this ?: 0

private val previewFilterState = UiState(
    sheetUrl = "preview",
    availableCards = listOf("Amex Gold", "Amex Platinum", "Chase Sapphire Reserve"),
    cardCounts = mapOf("Amex Gold" to 4, "Amex Platinum" to 6, "Chase Sapphire Reserve" to 3),
    allItems = emptyList(),
    selectedStatuses = DEFAULT_STATUS_FILTERS,
    statusCounts = PerkStatus.entries.associateWith { 1 }
)

@Preview(name = "Filters 320dp", widthDp = 320, heightDp = 180, showBackground = true)
@Composable
private fun FilterSectionPreview320() {
    com.perkz.ui.theme.PerkzTheme {
        FilterSection(previewFilterState, {}, {}, { _, _ -> }, {})
    }
}

@Preview(name = "Filters 360dp", widthDp = 360, heightDp = 180, showBackground = true)
@Composable
private fun FilterSectionPreview360() {
    com.perkz.ui.theme.PerkzTheme {
        FilterSection(previewFilterState, {}, {}, { _, _ -> }, {})
    }
}

@Preview(name = "Filters 390dp", widthDp = 390, heightDp = 180, showBackground = true)
@Composable
private fun FilterSectionPreview390() {
    com.perkz.ui.theme.PerkzTheme {
        FilterSection(previewFilterState, {}, {}, { _, _ -> }, {})
    }
}

private fun statusPreviewCount(uiState: UiState, selectedStatuses: Set<PerkStatus>): Int =
    when {
        selectedStatuses == DEFAULT_STATUS_FILTERS -> attentionCount(uiState)
        selectedStatuses.isAllStatusesSelection() -> uiState.allItems.size
        else -> selectedStatuses.sumOf { uiState.statusCounts[it].orZero() }
    }

@Composable
private fun statusIndicatorColor(selectedStatuses: Set<PerkStatus>): Color = when {
    selectedStatuses == DEFAULT_STATUS_FILTERS -> MaterialTheme.colorScheme.error
    selectedStatuses.size == 1 -> selectedStatuses.first().resolvedColors().accentColor
    selectedStatuses.isAllStatusesSelection() -> MaterialTheme.colorScheme.outline
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun statusDotColor(option: StatusPresetOption): Color = when (option.label) {
    ATTENTION_FILTER -> MaterialTheme.colorScheme.error
    ALL_STATUSES_FILTER -> MaterialTheme.colorScheme.outline
    else -> option.status?.resolvedColors()?.accentColor ?: MaterialTheme.colorScheme.outline
}

private fun statusOptionCount(uiState: UiState, option: StatusPresetOption): Int = when (option.label) {
    ATTENTION_FILTER -> attentionCount(uiState)
    ALL_STATUSES_FILTER -> uiState.allItems.size
    else -> option.statuses.sumOf { uiState.statusCounts[it].orZero() }
}

/** Statuses where a "$X remaining" style summary next to the section title is meaningful. */
private fun com.perkz.ui.model.UiStatusGroup.remainingSummary(): String? {
    if (!status.showsRemainingSummary) return null
    val items = intervalGroups.flatMap { it.items }
    val remainingValues = items.mapNotNull { item -> item.maxAmount?.let { (it - item.usedAmount).coerceAtLeast(0.0) } }
    if (remainingValues.isEmpty()) return null
    val allCountUnits = items.all { isCountUnit(it.perk.maxValueOrUses) }
    val prefix = if (allCountUnits) "" else "$"
    return "$prefix${formatAmount(remainingValues.sum())}"
}

@Composable
private fun PerkList(
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val listScope = this
        uiState.statusGroups
            .filter { it.intervalGroups.isNotEmpty() }
            .forEach statusLoop@{ statusGroup ->
            val collapsed = statusGroup.status in uiState.collapsedStatuses
            item(key = "header-${statusGroup.status.name}") {
                val statusColors = statusGroup.status.resolvedColors()
                if (collapsed) {
                    // Collapsed sections render as a compact neutral summary bar.
                    val perkCount = statusGroup.intervalGroups.sumOf { it.items.size }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onToggleStatusCollapsed(statusGroup.status) }
                            .semantics { stateDescription = "Collapsed" }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = statusGroup.status.label.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "  •  $perkCount perks",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Expanded sections show a rich title, subtitle, and optional remaining summary.
                    val remainingSummary = statusGroup.remainingSummary()
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleStatusCollapsed(statusGroup.status) }
                            .semantics { stateDescription = "Expanded" }
                            .padding(top = 6.dp, bottom = 4.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = statusGroup.status.label,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.Filled.ExpandLess,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = statusGroup.status.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (remainingSummary != null) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .width(1.dp)
                                    .height(48.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = remainingSummary,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColors.accentColor,
                                )
                                Text(
                                    text = "remaining",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            if (!collapsed) {
                listScope.addStatusItems(
                    statusGroup,
                    uiState.collapsedIntervals,
                    onToggleIntervalCollapsed,
                    onSetIntervalsCollapsed,
                    onToggleUsed,
                    onAmountAdded,
                    onMarkFull,
                    onClearUsage,
                    onNotApplicableChange
                )
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
    onToggleUsed: (PerkEntity, Boolean) -> Unit,
    onAmountAdded: (PerkEntity, Double) -> Unit,
    onMarkFull: (PerkEntity) -> Unit,
    onClearUsage: (PerkEntity) -> Unit,
    onNotApplicableChange: (PerkEntity, Boolean) -> Unit
) {
    val intervals = statusGroup.intervalGroups.map { "${statusGroup.status.name}|${it.interval}" }.toSet()
    if (statusGroup.intervalGroups.isEmpty()) {
        item(key = "empty-${statusGroup.status.name}") {
            Text(text = statusGroup.status.emptyText, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 18.dp, bottom = 4.dp))
        }
    } else {
        statusGroup.intervalGroups.forEach { intervalGroup ->
            val intervalKey = "${statusGroup.status.name}|${intervalGroup.interval}"
            val collapsed = intervalKey in collapsedIntervals
            val allCollapsed = collapsedIntervals.intersect(intervals).size == intervals.size
            item(key = "interval-${statusGroup.status.name}-${intervalGroup.interval}") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleIntervalCollapsed(intervalKey) }
                                .semantics {
                                    stateDescription = if (collapsed) "Collapsed" else "Expanded"
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${intervalGroup.interval.uppercase()}  •  ${intervalGroup.items.size} PERKS",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    onSetIntervalsCollapsed(intervals, !allCollapsed)
                                }
                            ) {
                                Text(if (allCollapsed) "Expand all" else "Collapse all")
                                Icon(
                                    if (allCollapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.ExpandLess,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (!collapsed) {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
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
            }
        }
    }
}
