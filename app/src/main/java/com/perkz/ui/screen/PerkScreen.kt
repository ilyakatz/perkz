package com.perkz.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.perkz.data.db.PerkEntity
import com.perkz.ui.model.NotificationSchedule
import com.perkz.ui.model.PerkStatus
import com.perkz.ui.model.ThemeMode
import com.perkz.ui.model.UiIntervalGroup
import com.perkz.ui.model.UiPerkItem
import com.perkz.ui.model.UiState
import com.perkz.ui.model.UiStatusGroup
import com.perkz.ui.theme.PerkzTheme
import com.perkz.viewmodel.PerkViewModel
import java.time.LocalTime

private enum class AppTab { Perks, Notifications, Settings }

@Composable
internal fun PerkScreen(viewModel: PerkViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    PerkScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onRefresh = viewModel::refresh,
        onCardSelect = viewModel::selectCard,
        onStatusPresetSelect = viewModel::selectStatusPreset,
        onSearchQueryChange = viewModel::setSearchQuery,
        onApplyFilters = viewModel::applyFilters,
        onClearFilters = viewModel::clearFilters,
        onToggleStatusCollapsed = viewModel::toggleStatusCollapsed,
        onToggleIntervalCollapsed = viewModel::toggleIntervalCollapsed,
        onSetIntervalsCollapsed = viewModel::setIntervalsCollapsed,
        onToggleUsed = viewModel::toggleUsed,
        onAmountAdded = viewModel::addUsage,
        onMarkFull = viewModel::markFull,
        onClearUsage = viewModel::clearUsage,
        onNotApplicableChange = viewModel::setNotApplicable,
        onSaveSchedule = viewModel::saveNotificationSettings,
        onTestNotification = viewModel::triggerTestNotification,
        onThemeModeChange = viewModel::saveThemeMode,
        onSaveSettings = viewModel::saveSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerkScreenContent(
    uiState: UiState,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onCardSelect: (String) -> Unit,
    onStatusPresetSelect: (Set<PerkStatus>) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onApplyFilters: (Set<String>, Set<PerkStatus>) -> Unit,
    onClearFilters: () -> Unit,
    onToggleStatusCollapsed: (PerkStatus) -> Unit,
    onToggleIntervalCollapsed: (String) -> Unit,
    onSetIntervalsCollapsed: (Set<String>, Boolean) -> Unit,
    onToggleUsed: (PerkEntity, Boolean) -> Unit,
    onAmountAdded: (PerkEntity, Double) -> Unit,
    onMarkFull: (PerkEntity) -> Unit,
    onClearUsage: (PerkEntity) -> Unit,
    onNotApplicableChange: (PerkEntity, Boolean) -> Unit,
    onSaveSchedule: (NotificationSchedule, LocalTime) -> Unit,
    onTestNotification: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onSaveSettings: (String, String) -> Unit
) {
    var urlInput by remember(uiState.sheetUrl) { mutableStateOf(uiState.sheetUrl) }
    var webhookInput by remember(uiState.webhookUrl) { mutableStateOf(uiState.webhookUrl) }
    var selectedTab by remember { mutableStateOf(AppTab.Perks) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Perkz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Card perks, fully yours.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selectedTab == AppTab.Perks) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.semantics {
                                contentDescription = "Refresh perks"
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                        Text(
                            if (uiState.isLoading) "Syncing…" else uiState.syncLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == AppTab.Perks,
                    onClick = { selectedTab = AppTab.Perks },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Perks"
                        )
                    },
                    label = { Text("Perks") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.Notifications,
                    onClick = { selectedTab = AppTab.Notifications },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications"
                        )
                    },
                    label = { Text("Notifications") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.Settings,
                    onClick = { selectedTab = AppTab.Settings },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                AppTab.Perks -> PerksTabContent(
                    uiState = uiState,
                    onCardSelect = onCardSelect,
                    onStatusPresetSelect = onStatusPresetSelect,
                    onSearchQueryChange = onSearchQueryChange,
                    onApplyFilters = onApplyFilters,
                    onClearFilters = onClearFilters,
                    onToggleStatusCollapsed = onToggleStatusCollapsed,
                    onToggleIntervalCollapsed = onToggleIntervalCollapsed,
                    onSetIntervalsCollapsed = onSetIntervalsCollapsed,
                    onToggleUsed = onToggleUsed,
                    onAmountAdded = onAmountAdded,
                    onMarkFull = onMarkFull,
                    onClearUsage = onClearUsage,
                    onNotApplicableChange = onNotApplicableChange
                )
                AppTab.Notifications -> NotificationsTabContent(
                    uiState = uiState,
                    onSaveSchedule = onSaveSchedule,
                    onTestNotification = onTestNotification
                )
                AppTab.Settings -> SettingsTabContent(
                    urlInput = urlInput,
                    webhookInput = webhookInput,
                    selectedThemeMode = uiState.themeMode,
                    isLoading = uiState.isLoading,
                    syncError = uiState.syncError,
                    onUrlChange = { urlInput = it },
                    onWebhookChange = { webhookInput = it },
                    onThemeModeChange = onThemeModeChange,
                    onSave = { onSaveSettings(urlInput, webhookInput) },
                    onRefresh = onRefresh
                )
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
            maxValueOrUses = "100",
            details = "Preview details",
            benefitUnit = "auto",
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

private val previewState = UiState(
    sheetUrl = "https://docs.google.com/spreadsheets/d/1",
    hasAnyPerks = true,
    availableCards = listOf("Amex Gold", "Chase Sapphire"),
    statusCounts = mapOf(PerkStatus.ExpiringSoon to 1, PerkStatus.NeedsUse to 1),
    statusGroups = listOf(
        UiStatusGroup(
            status = PerkStatus.ExpiringSoon,
            intervalGroups = listOf(
                UiIntervalGroup(
                    interval = "Monthly",
                    items = listOf(
                        previewPerk("1", "Dining credit", "Amex Gold", "Monthly", PerkStatus.ExpiringSoon, 20.0),
                    )
                )
            )
        )
    )
)

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun PerkScreenLightPreview() {
    PerkzTheme(themeMode = ThemeMode.LIGHT) {
        PerkScreenContent(
            uiState = previewState,
            snackbarHostState = remember { SnackbarHostState() },
            onRefresh = {},
            onCardSelect = {},
            onStatusPresetSelect = {},
            onSearchQueryChange = {},
            onApplyFilters = { _, _ -> },
            onClearFilters = {},
            onToggleStatusCollapsed = {},
            onToggleIntervalCollapsed = {},
            onSetIntervalsCollapsed = { _, _ -> },
            onToggleUsed = { _, _ -> },
            onAmountAdded = { _, _ -> },
            onMarkFull = {},
            onClearUsage = {},
            onNotApplicableChange = { _, _ -> },
            onSaveSchedule = { _, _ -> },
            onTestNotification = {},
            onThemeModeChange = {},
            onSaveSettings = { _, _ -> }
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun PerkScreenDarkPreview() {
    PerkzTheme(themeMode = ThemeMode.DARK) {
        PerkScreenContent(
            uiState = previewState,
            snackbarHostState = remember { SnackbarHostState() },
            onRefresh = {},
            onCardSelect = {},
            onStatusPresetSelect = {},
            onSearchQueryChange = {},
            onApplyFilters = { _, _ -> },
            onClearFilters = {},
            onToggleStatusCollapsed = {},
            onToggleIntervalCollapsed = {},
            onSetIntervalsCollapsed = { _, _ -> },
            onToggleUsed = { _, _ -> },
            onAmountAdded = { _, _ -> },
            onMarkFull = {},
            onClearUsage = {},
            onNotApplicableChange = { _, _ -> },
            onSaveSchedule = { _, _ -> },
            onTestNotification = {},
            onThemeModeChange = {},
            onSaveSettings = { _, _ -> }
        )
    }
}
