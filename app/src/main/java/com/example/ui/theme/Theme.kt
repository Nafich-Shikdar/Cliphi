package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ClipForgeDarkColorScheme = darkColorScheme(
    primary = VioletAccent,
    onPrimary = TextPrimary,
    primaryContainer = VioletDeepContainer,
    onPrimaryContainer = LavenderAccent,
    secondary = LavenderAccent,
    onSecondary = ObsidianBg,
    secondaryContainer = StudioCardElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = SuccessGreen,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = StudioCard,
    onSurface = TextPrimary,
    surfaceVariant = StudioCardElevated,
    onSurfaceVariant = TextMuted,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = StudioCard,
    surfaceContainer = StudioCard,
    surfaceContainerHigh = StudioCardElevated,
    surfaceContainerHighest = StudioSurfaceBright,
    outline = StudioOutline,
    outlineVariant = StudioOutline,
    error = ErrorRed,
    onError = TextPrimary,
    errorContainer = ErrorContainerDark,
    onErrorContainer = TextPrimary
)

val ClipForgeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ClipForgeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ClipForgeDarkColorScheme,
        typography = ClipForgeTypography,
        shapes = ClipForgeShapes,
        content = content
    )
}
