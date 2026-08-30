package com.perkz.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text(
                text = "Sheet connection",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            Text(
                text = "Use the CSV export URL from your Google Sheet",
                style = MaterialTheme.typography.bodySmall
            )
        }
        item {
            val urlValidationResult = validateCsvUrl(urlInput)
            val isValidUrl = urlValidationResult.isValid

            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Google Sheet CSV URL") },
                supportingText = {
                    if (!isValidUrl && urlInput.isNotBlank()) {
                        Text(
                            urlValidationResult.errorMessage,
                            color = Color(0xFFB3261E)
                        )
                    }
                },
                isError = !isValidUrl && urlInput.isNotBlank()
            )
        }
        item {
            val urlValidationResult = validateCsvUrl(urlInput)
            val isValidUrl = urlValidationResult.isValid

            OutlinedTextField(
                value = webhookInput,
                onValueChange = onWebhookChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Update webhook URL (Apps Script)") }
            )
        }
        item {
            val urlValidationResult = validateCsvUrl(urlInput)
            val isValidUrl = urlValidationResult.isValid

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSave,
                    enabled = isValidUrl
                ) {
                    Text("Save settings")
                }
                TextButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }
        }
    }
}
