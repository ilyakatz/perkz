package com.perkz.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.tooling.preview.Preview
import com.perkz.domain.BenefitUnit
import com.perkz.domain.currencyPrefixFor
import com.perkz.domain.formatUnitAmount
import com.perkz.domain.parseAmount
import com.perkz.ui.model.ThemeMode
import com.perkz.ui.theme.PerkzTheme

@Composable
internal fun RecordUsageDialog(
    amountText: String,
    unit: BenefitUnit,
    maxValueOrUses: String,
    maxAmount: Double?,
    usedAmount: Double,
    onAmountTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAmountAdded: (Double) -> Unit,
) {
    val enteredAmount = parseAmount(amountText)
    val dialogRemaining = maxAmount?.minus(usedAmount)
    val exceedsRemaining =
        dialogRemaining != null && enteredAmount != null && enteredAmount > dialogRemaining
    val remainingText = dialogRemaining?.let {
        formatUnitAmount(unit, maxValueOrUses, it.coerceAtLeast(0.0))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        RecordUsageDialogContent(
            amountText = amountText,
            unit = unit,
            maxValueOrUses = maxValueOrUses,
            remainingText = remainingText,
            enteredAmount = enteredAmount,
            exceedsRemaining = exceedsRemaining,
            onAmountTextChange = onAmountTextChange,
            onDismiss = onDismiss,
            onAmountAdded = onAmountAdded,
        )
    }
}

@Composable
private fun RecordUsageDialogContent(
    amountText: String,
    unit: BenefitUnit,
    maxValueOrUses: String,
    remainingText: String?,
    enteredAmount: Double?,
    exceedsRemaining: Boolean,
    onAmountTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAmountAdded: (Double) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.84f)
            .widthIn(max = 320.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Record usage", style = MaterialTheme.typography.titleLarge)
            Text(
                text = remainingText?.let { "$it remaining" }
                    ?: "Enter the amount you used.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = onAmountTextChange,
                leadingIcon = {
                    Text(
                        text = currencyPrefixFor(unit, maxValueOrUses).ifBlank { "#" },
                        fontWeight = FontWeight.Bold,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = enteredAmount == null && amountText.isNotBlank() || exceedsRemaining,
                supportingText = {
                    Text(
                        text = if (exceedsRemaining) {
                            "Only ${remainingText ?: "the remaining amount"} available."
                        } else {
                            remainingText?.let { "Enter up to $it" }
                                ?: "Enter the amount used"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (exceedsRemaining) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Cancel")
                }
                Button(
                    enabled = enteredAmount != null && enteredAmount > 0.0 && !exceedsRemaining,
                    onClick = { onAmountAdded(enteredAmount!!) },
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    modifier = Modifier.height(40.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) { Text("Save") }
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun RecordUsageDialogLightPreview() {
    PerkzTheme(themeMode = ThemeMode.LIGHT) {
        RecordUsageDialogContent(
            amountText = "25",
            unit = BenefitUnit.AUTO,
            maxValueOrUses = "$300",
            remainingText = "$175 remaining",
            enteredAmount = 25.0,
            exceedsRemaining = false,
            onAmountTextChange = {},
            onDismiss = {},
            onAmountAdded = {},
        )
    }
}

@Preview(
    name = "Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RecordUsageDialogDarkPreview() {
    PerkzTheme(themeMode = ThemeMode.DARK) {
        RecordUsageDialogContent(
            amountText = "25",
            unit = BenefitUnit.AUTO,
            maxValueOrUses = "$300",
            remainingText = "$175 remaining",
            enteredAmount = 25.0,
            exceedsRemaining = false,
            onAmountTextChange = {},
            onDismiss = {},
            onAmountAdded = {},
        )
    }
}
