package com.perkz.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perkz.viewmodel.PerkViewModel

private enum class AppTab { Perks, Settings }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PerkScreen(viewModel: PerkViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var urlInput by remember(uiState.sheetUrl) { mutableStateOf(uiState.sheetUrl) }
    var webhookInput by remember(uiState.webhookUrl) { mutableStateOf(uiState.webhookUrl) }
    var selectedTab by remember { mutableStateOf(AppTab.Perks) }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Perkz",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    if (selectedTab == AppTab.Perks) {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh perks"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
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
                            imageVector = Icons.Default.Star,
                            contentDescription = "Perks"
                        )
                    },
                    label = { Text("Perks") }
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
                    label = { Text("Settings") }
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
                    onCardSelect = viewModel::selectCard,
                    onStatusSelect = viewModel::selectStatusFilter,
                    onToggleUsed = viewModel::toggleUsed
                )
                AppTab.Settings -> SettingsTabContent(
                    urlInput = urlInput,
                    webhookInput = webhookInput,
                    selectedThemeMode = uiState.themeMode,
                    onUrlChange = { urlInput = it },
                    onWebhookChange = { webhookInput = it },
                    onThemeModeChange = viewModel::saveThemeMode,
                    onSave = { viewModel.saveSettings(urlInput, webhookInput) },
                    onRefresh = viewModel::refresh
                )
            }
        }
    }
}

