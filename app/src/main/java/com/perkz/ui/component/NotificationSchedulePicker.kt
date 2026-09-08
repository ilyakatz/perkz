package com.perkz.ui.component

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.perkz.ui.model.NotificationSchedule
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.ui.tooling.preview.Preview
import com.perkz.ui.model.ThemeMode
import com.perkz.ui.theme.PerkzTheme

@Composable
internal fun NotificationSchedulePicker(
    initialSchedule: NotificationSchedule,
    initialTime: LocalTime,
    onSaveSchedule: (NotificationSchedule, LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    var selectedSchedule by remember(initialSchedule) { mutableStateOf(initialSchedule) }
    var selectedTime by remember(initialTime) { mutableStateOf(initialTime) }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            selectedTime = LocalTime.of(hour, minute)
        },
        selectedTime.hour,
        selectedTime.minute,
        false
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Reminder Schedule",
            style = MaterialTheme.typography.titleMedium
        )

        NotificationSchedule.entries.forEach { schedule ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedSchedule == schedule,
                    onClick = { selectedSchedule = schedule }
                )
                Text(
                    text = schedule.label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        if (selectedSchedule != NotificationSchedule.Off) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Reminder Time",
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedButton(
                    onClick = { timePickerDialog.show() }
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = selectedTime.format(timeFormatter))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSaveSchedule(selectedSchedule, selectedTime) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Set Reminder Schedule")
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun NotificationSchedulePickerLightPreview() {
    PerkzTheme(themeMode = ThemeMode.LIGHT) {
        NotificationSchedulePicker(
            initialSchedule = NotificationSchedule.Daily,
            initialTime = LocalTime.of(9, 0),
            onSaveSchedule = { _, _ -> },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun NotificationSchedulePickerDarkPreview() {
    PerkzTheme(themeMode = ThemeMode.DARK) {
        NotificationSchedulePicker(
            initialSchedule = NotificationSchedule.Off,
            initialTime = LocalTime.of(20, 30),
            onSaveSchedule = { _, _ -> },
            modifier = Modifier.padding(16.dp)
        )
    }
}
