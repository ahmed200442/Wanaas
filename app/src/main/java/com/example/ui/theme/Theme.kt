package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = WanasAmberGold,
    onPrimary = Color(0xFF1A103C),
    primaryContainer = Color(0xFF3B2667),
    onPrimaryContainer = WanasAmberGold,
    secondary = WanasTealCalm,
    onSecondary = Color(0xFF00201D),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFF99F6E4),
    tertiary = WanasVioletAccent,
    onTertiary = Color.White,
    background = WanasDeepBg,
    onBackground = Color.White,
    surface = WanasCardBg,
    onSurface = Color.White,
    surfaceVariant = WanasSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFE2D9FF)
)

/**
 * Daytime Color Scheme (الوضع النهاري) combining bright luminous surfaces
 * (`WanasDayLightBg`, `WanasDayLightSurface`) with rich dark royal containers
 * (`WanasDayDarkHeader`, `WanasAmberGoldDeep`, `WanasTealDeep`) for high visual contrast.
 */
private val LightColorScheme = lightColorScheme(
    primary = WanasDayDarkHeader,
    onPrimary = WanasAmberGold,
    primaryContainer = WanasDayDarkHeader,
    onPrimaryContainer = Color.White,
    secondary = WanasTealDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = WanasDayDarkEmerald,
    tertiary = WanasAmberGoldDeep,
    onTertiary = Color.White,
    background = WanasDayLightBg,
    onBackground = WanasDayTextPrimary,
    surface = WanasDayLightSurface,
    onSurface = WanasDayTextPrimary,
    surfaceVariant = WanasDaySurfaceVariant,
    onSurfaceVariant = WanasDayTextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
