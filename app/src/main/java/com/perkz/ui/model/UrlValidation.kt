package com.perkz.ui.model

data class UrlValidationResult(
    val isValid: Boolean,
    val errorMessage: String = ""
)

internal fun validateCsvUrl(url: String): UrlValidationResult {
    if (url.isBlank()) {
        return UrlValidationResult(isValid = true)
    }

    if (!url.contains("docs.google.com/spreadsheets")) {
        return UrlValidationResult(
            isValid = false,
            errorMessage = "URL must be a Google Sheets link"
        )
    }

    if (!url.contains("/export")) {
        return UrlValidationResult(
            isValid = false,
            errorMessage = "URL must be a CSV export URL (add /export?format=csv)"
        )
    }

    if (!url.contains("format=csv")) {
        return UrlValidationResult(
            isValid = false,
            errorMessage = "URL must have format=csv parameter"
        )
    }

    return UrlValidationResult(isValid = true)
}
