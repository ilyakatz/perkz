package com.perkz.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.perkz.ui.model.ThemeMode

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

private val DarkColorScheme = darkColorScheme(
    primary = PerkzDarkPrimary,
    onPrimary = PerkzDarkOnPrimary,
    primaryContainer = PerkzDarkPrimaryContainer,
    onPrimaryContainer = PerkzDarkOnPrimaryContainer,
    secondary = PerkzDarkSecondary,
    onSecondary = PerkzDarkOnSecondary,
    secondaryContainer = PerkzDarkSecondaryContainer,
    onSecondaryContainer = PerkzDarkOnSecondaryContainer,
    tertiary = PerkzDarkTertiary,
    onTertiary = PerkzDarkOnTertiary,
    tertiaryContainer = PerkzDarkTertiaryContainer,
    onTertiaryContainer = PerkzDarkOnTertiaryContainer,
    background = PerkzDarkBackground,
    onBackground = PerkzDarkOnBackground,
    surface = PerkzDarkSurface,
    onSurface = PerkzDarkOnSurface,
    surfaceVariant = PerkzDarkSurfaceVariant,
    onSurfaceVariant = PerkzDarkOnSurfaceVariant,
    outline = PerkzDarkOutline,
    outlineVariant = PerkzDarkOutlineVariant,
    error = PerkzDarkError,
    onError = PerkzDarkOnError,
    errorContainer = PerkzDarkErrorContainer,
    onErrorContainer = PerkzDarkOnErrorContainer,
)

@Composable
fun PerkzTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
