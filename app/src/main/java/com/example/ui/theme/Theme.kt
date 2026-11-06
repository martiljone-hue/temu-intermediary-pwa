package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = TerracottaOrange,
    onPrimary = Color.White,
    primaryContainer = MazaganEmeraldLight,
    onPrimaryContainer = Color.White,
    secondary = MazaganGold,
    onSecondary = MazaganEmerald,
    tertiary = WhatsAppGreen,
    background = DarkBackground,
    onBackground = MazaganSand,
    surface = DarkSurface,
    onSurface = MazaganSand,
    surfaceVariant = DarkCard,
    onSurfaceVariant = Color(0xFFD0DDD8)
)

private val LightColorScheme = lightColorScheme(
    primary = MazaganEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EBE5),
    onPrimaryContainer = MazaganEmerald,
    secondary = TerracottaOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE5D6),
    onSecondaryContainer = TerracottaOrange,
    tertiary = WhatsAppGreen,
    background = MazaganSand,
    onBackground = TextDark,
    surface = MazaganSurface,
    onSurface = TextDark,
    surfaceVariant = MazaganCardCream,
    onSurfaceVariant = TextMuted,
    outline = MazaganBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Enforce 100% RTL (Right-to-Left) layout across the entire application
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
