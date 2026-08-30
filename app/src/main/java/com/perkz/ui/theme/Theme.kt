package com.perkz.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PerkzPrimary,
    onPrimary = PerkzOnPrimary,
    primaryContainer = PerkzPrimaryContainer,
    onPrimaryContainer = PerkzOnPrimaryContainer,
    secondary = PerkzSecondary,
    onSecondary = PerkzOnSecondary,
    secondaryContainer = PerkzSecondaryContainer,
    onSecondaryContainer = PerkzOnSecondaryContainer,
    tertiary = PerkzTertiary,
    onTertiary = PerkzOnTertiary,
    tertiaryContainer = PerkzTertiaryContainer,
    onTertiaryContainer = PerkzOnTertiaryContainer,
    background = PerkzBackground,
    onBackground = PerkzOnBackground,
    surface = PerkzSurface,
    onSurface = PerkzOnSurface,
    surfaceVariant = PerkzSurfaceVariant,
    onSurfaceVariant = PerkzOnSurfaceVariant,
    outline = PerkzOutline,
    outlineVariant = PerkzOutlineVariant,
    error = PerkzError,
    onError = PerkzOnError,
    errorContainer = PerkzErrorContainer,
    onErrorContainer = PerkzOnErrorContainer,
)

@Composable
fun PerkzTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
