package com.perkz.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import androidx.compose.ui.tooling.preview.Preview
import com.perkz.data.csv.PerkDraft
import com.perkz.data.csv.TableDataParser
import com.perkz.ui.theme.PerkzTheme
import com.perkz.ui.model.ThemeMode

private enum class AddMode { Single, Bulk }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddPerkScreen(
    availableCards: List<String>,
    onDismiss: () -> Unit,
    onAddPerk: (
        title: String,
        card: String,
        interval: String,
        maxValue: String,
        units: String,
        resetPeriod: String,
        deadline: String,
        details: String
    ) -> Unit,
    onAddSinglePerk: suspend (PerkDraft) -> Result<Unit> = { Result.success(Unit) },
    onCompleteBulk: () -> Unit = {}
) {
    BackHandler(onBack = onDismiss)

    var currentMode by remember { mutableStateOf(AddMode.Single) }

    val currentMonth = remember {
        java.time.LocalDate.now().month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.US)
    }

    val allMonths = remember {
        java.time.Month.entries.map { it.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.US) }
    }

    // Single Entry State
    var title by remember { mutableStateOf("") }
    var card by remember { mutableStateOf("") }
    var interval by remember { mutableStateOf("Monthly") }
    var maxValue by remember { mutableStateOf("") }
    var units by remember { mutableStateOf("USD") }
    var resetPeriod by remember { mutableStateOf(currentMonth) }
    var deadline by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }

    val intervals = listOf("Monthly", "Semi-annual", "Annual", "One-time enrollment", "Per eligible stay")

    val resetPeriods = when (interval) {
        "Monthly" -> allMonths + "End of month"
        "Semi-annual" -> listOf("January–June", "July–December")
        "Annual" -> listOf("Calendar year")
        "One-time enrollment" -> listOf("No stated expiration")
        "Per eligible stay" -> listOf("No stated reset")
        else -> allMonths + listOf("Calendar year", "No stated reset", "No stated expiration", "End of month")
    }

    LaunchedEffect(interval) {
        if (resetPeriod !in resetPeriods) {
            resetPeriod = resetPeriods.firstOrNull() ?: ""
        }
    }
    val unitOptions = listOf("USD", "Uses", "Passes", "Points", "Miles", "Nights", "Months", "Guests", "Access", "None")

    // Bulk Entry State
    var rawText by remember { mutableStateOf("") }
    var defaultCard by remember { mutableStateOf(availableCards.firstOrNull() ?: "") }
    var bulkDrafts by remember { mutableStateOf<List<PerkDraft>>(emptyList()) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (currentMode == AddMode.Single) "Add New Perk" else "Bulk Add Perks") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentMode == AddMode.Single) {
                        IconButton(
                            onClick = {
                                onAddPerk(title, card, interval, maxValue, units, resetPeriod, deadline, details)
                                onDismiss()
                            },
                            enabled = title.isNotBlank() && card.isNotBlank() && maxValue.isNotBlank()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = currentMode.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = currentMode == AddMode.Single,
                    onClick = { currentMode = AddMode.Single },
                    text = { Text("Single Entry") }
                )
                Tab(
                    selected = currentMode == AddMode.Bulk,
                    onClick = { currentMode = AddMode.Bulk },
                    text = { Text("Bulk Import (CSV/TSV)") }
                )
            }

            when (currentMode) {
                AddMode.Single -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Perk Title") },
                            placeholder = { Text("e.g. Dining Credit") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        EditableDropdownField(
                            label = "Card Name",
                            options = availableCards,
                            value = card,
                            onValueChange = { card = it },
                            placeholder = "e.g. Amex Gold",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EditableDropdownField(
                                label = "Interval",
                                options = intervals,
                                value = interval,
                                onValueChange = { interval = it },
                                placeholder = "e.g. Monthly",
                                modifier = Modifier.weight(1f)
                            )

                            EditableDropdownField(
                                label = "Reset Period",
                                options = resetPeriods,
                                value = resetPeriod,
                                onValueChange = { resetPeriod = it },
                                placeholder = "e.g. End of month",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = maxValue,
                                onValueChange = { maxValue = it },
                                label = { Text("Max Value / Uses") },
                                placeholder = { Text("e.g. 10") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )

                            DropdownField(
                                label = "Units",
                                options = unitOptions,
                                selected = units,
                                onSelected = { units = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = deadline,
                            onValueChange = { deadline = it },
                            label = { Text("Deadline Trigger (Optional)") },
                            placeholder = { Text("e.g. Last day of month") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = details,
                            onValueChange = { details = it },
                            label = { Text("Details (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )

                        Button(
                            onClick = {
                                onAddPerk(title, card, interval, maxValue, units, resetPeriod, deadline, details)
                                onDismiss()
                            },
                            enabled = title.isNotBlank() && card.isNotBlank() && maxValue.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        ) {
                            Text("Add Perk to Google Sheet")
                        }
                    }
                }
                AddMode.Bulk -> {
                    BulkAddPerkContent(
                        availableCards = availableCards,
                        drafts = bulkDrafts,
                        rawText = rawText,
                        defaultCard = defaultCard,
                        onRawTextChange = { rawText = it },
                        onDefaultCardChange = { defaultCard = it },
                        onParseText = {
                            val parsed = TableDataParser.parse(rawText, defaultCard)
                            bulkDrafts = parsed
                        },
                        onDraftChange = { index, updated ->
                            bulkDrafts = bulkDrafts.toMutableList().apply { set(index, updated) }
                        },
                        onDeleteDraft = { index ->
                            bulkDrafts = bulkDrafts.toMutableList().apply { removeAt(index) }
                        },
                        onAddBlankDraft = {
                            bulkDrafts = bulkDrafts + PerkDraft(card = defaultCard)
                        },
                        onClearTable = {
                            bulkDrafts = emptyList()
                        },
                        onAddSinglePerk = onAddSinglePerk,
                        onCompleteBulk = onCompleteBulk
                    )
                }
            }
        }
    }
}

@Preview(name = "Add Perk Light", showBackground = true)
@Composable
private fun AddPerkScreenLightPreview() {
    PerkzTheme(themeMode = ThemeMode.LIGHT) {
        AddPerkScreen(
            availableCards = listOf("Amex Gold", "Chase Sapphire"),
            onDismiss = {},
            onAddPerk = { _, _, _, _, _, _, _, _ -> },
            onAddSinglePerk = { Result.success(Unit) },
            onCompleteBulk = {}
        )
    }
}

@Preview(name = "Add Perk Dark", showBackground = true)
@Composable
private fun AddPerkScreenDarkPreview() {
    PerkzTheme(themeMode = ThemeMode.DARK) {
        AddPerkScreen(
            availableCards = listOf("Amex Gold", "Chase Sapphire"),
            onDismiss = {},
            onAddPerk = { _, _, _, _, _, _, _, _ -> },
            onAddSinglePerk = { Result.success(Unit) },
            onCompleteBulk = {}
        )
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
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredOptions = remember(value, options) {
        if (options.contains(value)) options else options.filter { it.contains(value, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { 
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) 
            },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
        )
        
        if (options.isNotEmpty()) {
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
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
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
