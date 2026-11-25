package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = InfoAnimalPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF451A03),
    onPrimaryContainer = Color(0xFFFFEDD5),
    secondary = InfoAnimalSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = InfoAnimalAccent,
    onTertiary = Color.White,
    background = InfoAnimalBgDark,
    onBackground = InfoAnimalFgDark,
    surface = InfoAnimalMutedDark,
    onSurface = InfoAnimalFgDark,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = InfoAnimalSubtleDark,
    surfaceContainerLowest = Color(0xFF0B1120),
    surfaceContainerLow = Color(0xFF151F32),
    surfaceContainer = InfoAnimalMutedDark,
    surfaceContainerHigh = Color(0xFF263348),
    surfaceContainerHighest = Color(0xFF334155),
    outline = InfoAnimalSubtleDark,
    outlineVariant = InfoAnimalOutlineDark,
    error = InfoAnimalDestructive,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = InfoAnimalPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = Color(0xFF7C2D12),
    secondary = InfoAnimalSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = InfoAnimalAccent,
    onTertiary = Color.White,
    background = InfoAnimalBgLight,
    onBackground = InfoAnimalFgLight,
    surface = Color.White,
    onSurface = InfoAnimalFgLight,
    surfaceVariant = InfoAnimalMutedLight,
    onSurfaceVariant = InfoAnimalSubtleLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFCBD5E1),
    outline = InfoAnimalSubtleLight,
    outlineVariant = InfoAnimalOutlineLight,
    error = InfoAnimalDestructive,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
