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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perkz.ui.model.NotificationSchedule
import com.perkz.ui.model.UiState
import java.time.LocalTime
import androidx.compose.ui.tooling.preview.Preview
import com.perkz.ui.component.NotificationSchedulePicker
import com.perkz.ui.model.ThemeMode
import com.perkz.ui.theme.PerkzTheme

@Composable
internal fun NotificationsTabContent(
    uiState: UiState,
    onSaveSchedule: (NotificationSchedule, LocalTime) -> Unit,
    onTestNotification: () -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onTestNotification()
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
        
        NotificationSchedulePicker(
            initialSchedule = uiState.notificationSchedule,
            initialTime = uiState.notificationTime,
            onSaveSchedule = onSaveSchedule
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onTestNotification()
                }
            }
        ) {
            Text("Send Test Notification")
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun NotificationsTabContentLightPreview() {
    PerkzTheme(themeMode = ThemeMode.LIGHT) {
        NotificationsTabContent(
            uiState = UiState(
                notificationSchedule = NotificationSchedule.Daily,
                notificationTime = LocalTime.of(9, 0)
            ),
            onSaveSchedule = { _, _ -> },
            onTestNotification = {}
        )
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun NotificationsTabContentDarkPreview() {
    PerkzTheme(themeMode = ThemeMode.DARK) {
        NotificationsTabContent(
            uiState = UiState(
                notificationSchedule = NotificationSchedule.Off,
                notificationTime = LocalTime.of(20, 30)
            ),
            onSaveSchedule = { _, _ -> },
            onTestNotification = {}
        )
    }
}
