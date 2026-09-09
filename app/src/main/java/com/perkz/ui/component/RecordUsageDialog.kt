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
import com.perkz.domain.validateUsage
import com.perkz.ui.model.ThemeMode
import com.perkz.ui.theme.PerkzTheme

@Composable
internal fun RecordUsageDialog(
    amountText: String,
    unit: BenefitUnit,
    maxValueOrUses: String,
    maxAmount: Double?,
    usedAmount: Double,
    isSubtraction: Boolean = false,
    onAmountTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAmountAdded: (Double) -> Unit,
) {
    val validation = validateUsage(amountText, usedAmount, maxAmount, isSubtraction)
    val enteredAmount = parseAmount(amountText)
    val limitText = validation.limit?.let {
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
            isSubtraction = isSubtraction,
            limit = validation.limit,
            limitText = limitText,
            enteredAmount = enteredAmount,
            exceedsLimit = validation.exceedsLimit,
            showQuickActions = validation.showQuickActions,
            onAmountTextChange = onAmountTextChange,
            onDismiss = onDismiss,
            onAmountConfirmed = {
                onAmountAdded(if (isSubtraction) -it else it)
            },
        )
    }
}

@Composable
private fun RecordUsageDialogContent(
    amountText: String,
    unit: BenefitUnit,
    maxValueOrUses: String,
    isSubtraction: Boolean,
    limit: Double?,
    limitText: String?,
    enteredAmount: Double?,
    exceedsLimit: Boolean,
    showQuickActions: Boolean,
    onAmountTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAmountConfirmed: (Double) -> Unit,
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
            Text(
                text = if (isSubtraction) "Subtract usage" else "Record usage",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = when {
                    limitText != null && isSubtraction -> "$limitText already used."
                    limitText != null -> "$limitText remaining."
                    isSubtraction -> "Enter the amount to subtract."
                    else -> "Enter the amount you used."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showQuickActions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 1..limit!!.toInt()) {
                        Button(
                            onClick = { onAmountConfirmed(i.toDouble()) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(
                                text = i.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
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
                    isError = enteredAmount == null && amountText.isNotBlank() || exceedsLimit,
                    supportingText = {
                        Text(
                            text = if (exceedsLimit) {
                                if (isSubtraction) "Cannot subtract more than what was used ($limitText)."
                                else "Only ${limitText ?: "the remaining amount"} available."
                            } else {
                                limitText?.let { "Enter up to $it" }
                                    ?: if (isSubtraction) "Enter the amount to subtract"
                                    else "Enter the amount used"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (exceedsLimit) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
                if (!showQuickActions) {
                    Button(
                        enabled = enteredAmount != null && enteredAmount > 0.0 && !exceedsLimit,
                        onClick = { onAmountConfirmed(enteredAmount!!) },
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
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun RecordUsageDialogLightPreview() {
    PerkzTheme(themeMode = ThemeMode.LIGHT) {
        RecordUsageDialogContent(
            amountText = "25",
            unit = BenefitUnit.AUTO,
            maxValueOrUses = "$300",
            isSubtraction = false,
            limit = 175.0,
            limitText = "$175",
            enteredAmount = 25.0,
            exceedsLimit = false,
            showQuickActions = false,
            onAmountTextChange = {},
            onDismiss = {},
            onAmountConfirmed = {},
        )
    }
}

@Preview(name = "Buttons Light", showBackground = true)
@Composable
private fun RecordUsageDialogButtonsLightPreview() {
    PerkzTheme(themeMode = ThemeMode.LIGHT) {
        RecordUsageDialogContent(
            amountText = "3",
            unit = BenefitUnit.NONE,
            maxValueOrUses = "5 uses",
            isSubtraction = false,
            limit = 3.0,
            limitText = "3",
            enteredAmount = 3.0,
            exceedsLimit = false,
            showQuickActions = true,
            onAmountTextChange = {},
            onDismiss = {},
            onAmountConfirmed = {},
        )
    }
}

@Preview(name = "Subtract Light", showBackground = true)
@Composable
private fun SubtractUsageDialogLightPreview() {
    PerkzTheme(themeMode = ThemeMode.LIGHT) {
        RecordUsageDialogContent(
            amountText = "25",
            unit = BenefitUnit.AUTO,
            maxValueOrUses = "$300",
            isSubtraction = true,
            limit = 75.0,
            limitText = "$75",
            enteredAmount = 25.0,
            exceedsLimit = false,
            showQuickActions = false,
            onAmountTextChange = {},
            onDismiss = {},
            onAmountConfirmed = {},
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
            isSubtraction = false,
            limit = 175.0,
            limitText = "$175",
            enteredAmount = 25.0,
            exceedsLimit = false,
            showQuickActions = false,
            onAmountTextChange = {},
            onDismiss = {},
            onAmountConfirmed = {},
        )
    }
}

