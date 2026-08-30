package com.perkz.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.perkz.ui.model.UiPerkItem

@Composable
internal fun PerkRow(item: UiPerkItem, onCheckedChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = item.status.cardColor)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isUsedThisPeriod,
                onCheckedChange = onCheckedChange
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = item.status.badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = item.status.titleColor
                )
                Text(item.perk.title, fontWeight = FontWeight.SemiBold)
                if (item.perk.card.isNotBlank()) {
                    Text(item.perk.card, style = MaterialTheme.typography.bodyMedium)
                }
                if (item.perk.maxValueOrUses.isNotBlank()) {
                    Text("Value: ${item.perk.maxValueOrUses}", style = MaterialTheme.typography.bodySmall)
                }
                if (item.resetPeriodLabel.isNotBlank()) {
                    Text("Reset: ${item.resetPeriodLabel}", style = MaterialTheme.typography.bodySmall)
                }
                if (item.perk.deadlineTrigger.isNotBlank()) {
                    Text("Deadline: ${item.perk.deadlineTrigger}", style = MaterialTheme.typography.bodySmall)
                }
                Text("Tracking period: ${item.periodLabel}", style = MaterialTheme.typography.bodySmall)
                if (item.perk.details.isNotBlank()) {
                    Text(item.perk.details, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
