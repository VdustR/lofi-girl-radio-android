package com.vdustr.lofiradio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LofiDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Background,
    primaryContainer = PrimaryDim,
    onPrimaryContainer = TextPrimary,
    secondary = Secondary,
    onSecondary = Background,
    tertiary = Accent,
    onTertiary = Background,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorColor,
    onError = Background,
    outline = Border,
    outlineVariant = BorderStrong
)

@Composable
fun LofiRadioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LofiDarkColorScheme,
        typography = Typography,
        content = content
    )
}
