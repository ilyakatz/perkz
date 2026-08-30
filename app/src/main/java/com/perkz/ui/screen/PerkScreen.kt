package com.perkz.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.perkz.viewmodel.PerkViewModel

private enum class AppTab {
    Perks,
    Settings
}

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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Credit Card Perks",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.zIndex(10f)
            ) {
                FilterChip(
                    selected = selectedTab == AppTab.Perks,
                    onClick = { selectedTab = AppTab.Perks },
                    label = { Text("Perks") }
                )
                FilterChip(
                    selected = selectedTab == AppTab.Settings,
                    onClick = { selectedTab = AppTab.Settings },
                    label = { Text("Settings") }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    AppTab.Perks -> PerksTabContent(
                        uiState = uiState,
                        onCardSelect = viewModel::selectCard,
                        onStatusSelect = viewModel::selectStatusFilter,
                        onToggleUsed = viewModel::toggleUsed,
                        onRefresh = viewModel::refresh
                    )
                    AppTab.Settings -> SettingsTabContent(
                        urlInput = urlInput,
                        webhookInput = webhookInput,
                        onUrlChange = { urlInput = it },
                        onWebhookChange = { webhookInput = it },
                        onSave = { viewModel.saveSettings(urlInput, webhookInput) },
                        onRefresh = viewModel::refresh
                    )
                }
            }
        }
    }
}
