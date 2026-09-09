package com.perkz.domain

internal data class UsageValidationResult(
    val limit: Double?,
    val exceedsLimit: Boolean,
    val isValid: Boolean
)

internal fun validateUsage(
    amountText: String,
    usedAmount: Double,
    maxAmount: Double?,
    isSubtraction: Boolean
): UsageValidationResult {
    val enteredAmount = parseAmount(amountText)
    val limit = if (isSubtraction) usedAmount else maxAmount?.minus(usedAmount)
    val exceedsLimit = limit != null && enteredAmount != null && enteredAmount > limit
    val isValid = enteredAmount != null && enteredAmount > 0.0 && !exceedsLimit
    
    return UsageValidationResult(limit, exceedsLimit, isValid)
}
