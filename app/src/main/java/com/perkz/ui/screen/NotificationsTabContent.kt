package com.perkz.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.perkz.notification.PerkNotificationManager
import com.perkz.ui.model.PerkStatus
import com.perkz.ui.model.UiState
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
internal fun NotificationsTabContent(uiState: UiState) {
    val context = LocalContext.current
    val notificationManager = PerkNotificationManager(context)

    fun triggerNotification() {
        val expiringSoon = uiState.allItems.filter { it.status == PerkStatus.ExpiringSoon }
        if (expiringSoon.isEmpty()) {
            notificationManager.showNotification(
                "Perkz",
                "You're all caught up! No perks are expiring soon."
            )
        } else {
            expiringSoon.forEach { item ->
                val sb = StringBuilder()
                val today = LocalDate.now()
                val daysLeft = ChronoUnit.DAYS.between(today, today.withDayOfMonth(today.lengthOfMonth()))
                
                sb.append("⏳ ${daysLeft.coerceAtLeast(0)} days left • ${item.perk.interval} benefit<br>")
                
                if (item.perk.deadlineTrigger.isNotBlank()) {
                    sb.append("⚠️ <b>Deadline:</b> ${item.perk.deadlineTrigger}<br>")
                }
                if (item.perk.details.isNotBlank()) {
                    sb.append("ℹ️ ${item.perk.details}")
                }
                
                val styledMessage = HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
                notificationManager.showNotification(
                    title = "${item.perk.title} (${item.perk.card})",
                    message = styledMessage,
                    notificationId = item.perk.sourceRowNumber
                )
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            triggerNotification()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Test perk reminders and schedule future alerts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    triggerNotification()
                }
            }
        ) {
            Text("Send Test Notification")
        }
    }
}
