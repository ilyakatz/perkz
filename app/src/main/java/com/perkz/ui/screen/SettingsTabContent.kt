package com.perkz.ui.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.perkz.ui.model.validateCsvUrl

@Composable
internal fun SettingsTabContent(
    urlInput: String,
    webhookInput: String,
    onUrlChange: (String) -> Unit,
    onWebhookChange: (String) -> Unit,
    onSave: () -> Unit,
    onRefresh: () -> Unit
) {
    val urlValidation = validateCsvUrl(urlInput)
    val isValidUrl = urlValidation.isValid

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Getting-started info card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Column {
                        Text(
                            text = "Getting started",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Open your Google Sheet → File → Share → Publish to web → select CSV format. Paste the published URL below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Sheet URL section
        item {
            Text(
                text = "Sheet connection",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Google Sheet CSV URL") },
                supportingText = {
                    if (!isValidUrl && urlInput.isNotBlank()) {
                        Text(urlValidation.errorMessage)
                    }
                },
                isError = !isValidUrl && urlInput.isNotBlank()
            )
        }

        // Sync settings section
        item {
            Text(
                text = "Sync settings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            OutlinedTextField(
                value = webhookInput,
                onValueChange = onWebhookChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Webhook URL (optional)") },
                supportingText = {
                    Text(
                        text = "Apps Script webhook to sync changes back to your sheet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        // Action buttons
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSave,
                    enabled = isValidUrl
                ) {
                    Text("Save settings")
                }
                OutlinedButton(onClick = onRefresh) {
                    Text("Refresh data")
                }
            }
        }
    }
}

