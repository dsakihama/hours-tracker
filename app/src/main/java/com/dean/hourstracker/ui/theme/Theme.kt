package com.dean.hourstracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary              = DeepTeal,
    onPrimary            = White,
    primaryContainer     = Mist,
    onPrimaryContainer   = DeepTeal,
    secondary            = Sage,
    onSecondary          = White,
    background           = Linen,
    onBackground         = Ink,
    surface              = White,
    onSurface            = Ink,
    surfaceVariant       = Mist,
    onSurfaceVariant     = Slate,
    outline              = BorderDk,
    outlineVariant       = BorderLt,
)

@Composable
fun HoursTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = AppTypography,
        content     = content,
    )
}
