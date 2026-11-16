package com.battleground.royale3d.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val TacticalDarkColorScheme = darkColorScheme(
    primary = BlazeAmber,
    onPrimary = ObsidianBg,
    primaryContainer = BlazeAmberDark,
    onPrimaryContainer = TextPrimary,
    secondary = CyberCyan,
    onSecondary = ObsidianBg,
    secondaryContainer = GunmetalElevated,
    onSecondaryContainer = CyberCyan,
    tertiary = SafeEmerald,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = GunmetalSurface,
    onSurface = TextPrimary,
    surfaceVariant = GunmetalElevated,
    onSurfaceVariant = TextSecondary,
    outline = TacticalBorder,
    error = CrimsonPulse,
    onError = TextPrimary
)

private val TacticalShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun BattleRoyaleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TacticalDarkColorScheme,
        typography = AppTypography,
        shapes = TacticalShapes,
        content = content
    )
}
