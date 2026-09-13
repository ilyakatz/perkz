package com.perkz.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.perkz.data.csv.DraftSyncState
import com.perkz.data.csv.PerkDraft
import com.perkz.ui.model.ThemeMode
import com.perkz.ui.theme.PerkzTheme
import kotlinx.coroutines.launch

@Composable
internal fun BulkAddPerkContent(
    availableCards: List<String>,
    drafts: List<PerkDraft>,
    rawText: String,
    defaultCard: String,
    onRawTextChange: (String) -> Unit,
    onDefaultCardChange: (String) -> Unit,
    onParseText: () -> Unit,
    onDraftChange: (Int, PerkDraft) -> Unit,
    onDeleteDraft: (Int) -> Unit,
    onAddBlankDraft: () -> Unit,
    onClearTable: () -> Unit,
    onAddSinglePerk: suspend (PerkDraft) -> Result<Unit>,
    onCompleteBulk: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }

    val totalValid = remember(drafts) { drafts.count { it.isValid } }
    val addedCount = remember(drafts) { drafts.count { it.syncState == DraftSyncState.ADDED } }
    val pendingCount = remember(drafts) { drafts.count { it.isValid && it.syncState != DraftSyncState.ADDED } }

    var isPasteExpanded by remember { mutableStateOf(drafts.isEmpty()) }

    LaunchedEffect(drafts.size) {
        if (drafts.isNotEmpty()) {
            isPasteExpanded = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Collapsible Raw Paste Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSubmitting) { isPasteExpanded = !isPasteExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Paste Table, CSV, or TSV Data",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isPasteExpanded && drafts.isNotEmpty()) {
                            Text(
                                text = "${drafts.size} rows parsed • Tap to expand input",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = { isPasteExpanded = !isPasteExpanded },
                        enabled = !isSubmitting,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isPasteExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isPasteExpanded) "Collapse paste input" else "Expand paste input"
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isPasteExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    ) {
                        EditableDropdownField(
                            label = "Default Card Name",
                            options = availableCards,
                            value = defaultCard,
                            onValueChange = onDefaultCardChange,
                            placeholder = "e.g. Citi Gold",
                            enabled = !isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = rawText,
                            onValueChange = onRawTextChange,
                            enabled = !isSubmitting,
                            label = { Text("Copied Table Content") },
                            placeholder = {
                                Text("Paste copied cells from web page, Excel, or CSV/TSV:\nBenefit\tCadence\tReset period\tMax value\nDining\tMonthly\tEnd of month\t10")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 70.dp, max = 110.dp),
                            minLines = 2
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (rawText.isNotBlank()) {
                                TextButton(
                                    onClick = { onRawTextChange("") },
                                    enabled = !isSubmitting
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Clear Input")
                                }
                            } else {
                                Spacer(Modifier.width(1.dp))
                            }

                            Button(
                                onClick = {
                                    onParseText()
                                    isPasteExpanded = false
                                },
                                enabled = rawText.isNotBlank() && !isSubmitting
                            ) {
                                Text("Parse Table")
                            }
                        }
                    }
                }
            }
        }

        // Table / Draft List Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Parsed Perks Table",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        drafts.isEmpty() -> "No perks parsed yet"
                        addedCount > 0 -> "$addedCount of $totalValid perks added to sheet"
                        else -> "$pendingCount of ${drafts.size} valid perks ready"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (addedCount == totalValid && totalValid > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onAddBlankDraft,
                    enabled = !isSubmitting
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Row")
                }
                if (drafts.isNotEmpty()) {
                    TextButton(
                        onClick = onClearTable,
                        enabled = !isSubmitting
                    ) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Maximized Scrollable Table Rows List
        if (drafts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Paste tabular data above and tap 'Parse Table', or tap '+ Add Row' to build manually.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                itemsIndexed(drafts, key = { _, item -> item.id }) { index, draft ->
                    BulkPerkRowCard(
                        index = index + 1,
                        draft = draft,
                        availableCards = availableCards,
                        onChange = { updated -> onDraftChange(index, updated) },
                        onDelete = { onDeleteDraft(index) }
                    )
                }
            }
        }

        // Bottom Action Button
        val buttonText = when {
            isSubmitting -> "Adding Perks to Sheet..."
            addedCount == totalValid && totalValid > 0 -> "All $totalValid Perks Added to Sheet"
            addedCount > 0 && pendingCount > 0 -> "Add Remaining $pendingCount Perks to Sheet"
            else -> "Add $pendingCount Perks to Google Sheet"
        }

        Button(
            onClick = {
                if (!isSubmitting && pendingCount > 0) {
                    coroutineScope.launch {
                        isSubmitting = true
                        drafts.forEachIndexed { index, draft ->
                            if (draft.isValid && draft.syncState != DraftSyncState.ADDED) {
                                onDraftChange(index, draft.copy(syncState = DraftSyncState.ADDING, errorMessage = null))
                                val result = onAddSinglePerk(draft)
                                if (result.isSuccess) {
                                    onDraftChange(index, draft.copy(syncState = DraftSyncState.ADDED, errorMessage = null))
                                } else {
                                    val err = result.exceptionOrNull()?.message ?: "Failed to add row"
                                    onDraftChange(index, draft.copy(syncState = DraftSyncState.ERROR, errorMessage = err))
                                }
                            }
                        }
                        isSubmitting = false
                        onCompleteBulk()
                    }
                }
            },
            enabled = !isSubmitting && pendingCount > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(buttonText)
        }
    }
}

@Composable
private fun BulkPerkRowCard(
    index: Int,
    draft: PerkDraft,
    availableCards: List<String>,
    onChange: (PerkDraft) -> Unit,
    onDelete: () -> Unit
) {
    val intervals = remember { listOf("Monthly", "Semi-annual", "Annual", "One-time enrollment", "Per eligible stay") }
    val unitOptions = remember { listOf("USD", "Uses", "Passes", "Points", "Miles", "Nights", "Months", "Guests", "Access", "None") }

    val isAdded = draft.syncState == DraftSyncState.ADDED
    val isAdding = draft.syncState == DraftSyncState.ADDING
    val isError = draft.syncState == DraftSyncState.ERROR
    val isEditable = !isAdded && !isAdding

    val containerColor = when {
        isAdded -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        draft.isValid -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
    }

    val borderColor = when {
        isAdded -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        isError -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        !draft.isValid -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = borderColor?.let { androidx.compose.foundation.BorderStroke(1.dp, it) }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "#$index",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isAdded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    when {
                        isAdded -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Added to sheet",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Added to Sheet",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        isAdding -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Adding to Sheet...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        isError -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Failed",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = draft.errorMessage ?: "Failed to add",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        draft.isValid -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Ready",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Ready to add",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        else -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Incomplete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Incomplete",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                if (!isAdded) {
                    IconButton(
                        onClick = onDelete,
                        enabled = isEditable,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete row",
                            tint = if (isEditable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // Title & Card
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { onChange(draft.copy(title = it)) },
                    enabled = isEditable,
                    label = { Text("Perk Title*") },
                    placeholder = { Text("e.g. Dining Credit") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                EditableDropdownField(
                    label = "Card*",
                    options = availableCards,
                    value = draft.card,
                    onValueChange = { onChange(draft.copy(card = it)) },
                    placeholder = "Card name",
                    enabled = isEditable,
                    modifier = Modifier.weight(1f)
                )
            }

            // Interval & Reset Period
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditableDropdownField(
                    label = "Interval",
                    options = intervals,
                    value = draft.interval,
                    onValueChange = { onChange(draft.copy(interval = it)) },
                    placeholder = "Monthly",
                    enabled = isEditable,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = draft.resetPeriod,
                    onValueChange = { onChange(draft.copy(resetPeriod = it)) },
                    enabled = isEditable,
                    label = { Text("Reset Period") },
                    placeholder = { Text("e.g. End of month") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Max Value & Units
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.maxValue,
                    onValueChange = { onChange(draft.copy(maxValue = it)) },
                    enabled = isEditable,
                    label = { Text("Max Value*") },
                    placeholder = { Text("e.g. 10") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                DropdownField(
                    label = "Units",
                    options = unitOptions,
                    selected = draft.units,
                    onSelected = { onChange(draft.copy(units = it)) },
                    enabled = isEditable,
                    modifier = Modifier.weight(1f)
                )
            }

            // Deadline & Details
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.deadline,
                    onValueChange = { onChange(draft.copy(deadline = it)) },
                    enabled = isEditable,
                    label = { Text("Deadline Trigger") },
                    placeholder = { Text("e.g. Last day of month") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = draft.details,
                    onValueChange = { onChange(draft.copy(details = it)) },
                    enabled = isEditable,
                    label = { Text("Details") },
                    placeholder = { Text("Notes...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableDropdownField(
    label: String,
    options: List<String>,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredOptions = remember(value, options) {
        if (options.contains(value)) options else options.filter { it.contains(value, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                if (enabled) {
                    onValueChange(it)
                    expanded = true
                }
            },
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            singleLine = true,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
        )

        if (options.isNotEmpty() && enabled) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                val displayOptions = if (value.isEmpty() || options.contains(value)) options else filteredOptions
                displayOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            enabled = enabled,
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            singleLine = true,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        if (enabled) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Preview(name = "Bulk Add Perks Light", showBackground = true)
@Composable
private fun BulkAddPerkContentLightPreview() {
    PerkzTheme(themeMode = ThemeMode.LIGHT) {
        BulkAddPerkContent(
            availableCards = listOf("Amex Gold", "Chase Sapphire"),
            drafts = listOf(
                PerkDraft(title = "Dining Credit", card = "Amex Gold", interval = "Monthly", maxValue = "10", units = "USD", syncState = DraftSyncState.ADDED),
                PerkDraft(title = "Uber Credit", card = "Amex Gold", interval = "Monthly", maxValue = "15", syncState = DraftSyncState.PENDING),
                PerkDraft(title = "", card = "", maxValue = "")
            ),
            rawText = "Benefit\tCadence\tMax value\nDining\tMonthly\t10",
            defaultCard = "Amex Gold",
            onRawTextChange = {},
            onDefaultCardChange = {},
            onParseText = {},
            onDraftChange = { _, _ -> },
            onDeleteDraft = {},
            onAddBlankDraft = {},
            onClearTable = {},
            onAddSinglePerk = { Result.success(Unit) },
            onCompleteBulk = {}
        )
    }
}

@Preview(name = "Bulk Add Perks Dark", showBackground = true)
@Composable
private fun BulkAddPerkContentDarkPreview() {
    PerkzTheme(themeMode = ThemeMode.DARK) {
        BulkAddPerkContent(
            availableCards = listOf("Amex Gold", "Chase Sapphire"),
            drafts = listOf(
                PerkDraft(title = "Dining Credit", card = "Amex Gold", interval = "Monthly", maxValue = "10", units = "USD", syncState = DraftSyncState.ADDED)
            ),
            rawText = "",
            defaultCard = "",
            onRawTextChange = {},
            onDefaultCardChange = {},
            onParseText = {},
            onDraftChange = { _, _ -> },
            onDeleteDraft = {},
            onAddBlankDraft = {},
            onClearTable = {},
            onAddSinglePerk = { Result.success(Unit) },
            onCompleteBulk = {}
        )
    }
}
