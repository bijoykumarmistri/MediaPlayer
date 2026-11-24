package com.mediaplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ============================================================================
// Material You 3 Dynamic Palette Accessors
// These properties dynamically adapt to the active Material 3 ColorScheme,
// seamlessly reflecting system wallpaper colors on Android 12+ (API 31+)
// and reacting to system dark / light theme changes throughout the entire app.
// ============================================================================

val AudiophileCyan: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

val AudiophileGold: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.tertiary

val AudiophileGoldDark: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.tertiary

val AudiophileGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.secondary

val AudiophilePurple: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.tertiary

val AudiophilePink: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.secondary

val OledBlack: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFF000000) else MaterialTheme.colorScheme.background

val OledSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val OledSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val OledSurfaceContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceContainer

val OledBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outlineVariant

// ============================================================================
// M3 Fallback Palettes (used on Android < 12 or when dynamic color is unavailable)
// Designed in accordance with official Material Design 3 tonal palettes.
// ============================================================================

// M3 Fallback Dark Colors
val DarkPrimary = Color(0xFF90CAF9)
val DarkOnPrimary = Color(0xFF003258)
val DarkPrimaryContainer = Color(0xFF004881)
val DarkOnPrimaryContainer = Color(0xFFD1E4FF)

val DarkSecondary = Color(0xFFBBC7DB)
val DarkOnSecondary = Color(0xFF253140)
val DarkSecondaryContainer = Color(0xFF3B4858)
val DarkOnSecondaryContainer = Color(0xFFD7E3F7)

val DarkTertiary = Color(0xFFD6BEE4)
val DarkOnTertiary = Color(0xFF3B2948)
val DarkTertiaryContainer = Color(0xFF523F5F)
val DarkOnTertiaryContainer = Color(0xFFF2DAFF)

val DarkBackground = Color(0xFF111318)
val DarkOnBackground = Color(0xFFE2E2E9)
val DarkSurface = Color(0xFF111318)
val DarkOnSurface = Color(0xFFE2E2E9)
val DarkSurfaceVariant = Color(0xFF44474F)
val DarkOnSurfaceVariant = Color(0xFFC4C6D0)
val DarkOutline = Color(0xFF8E9099)
val DarkOutlineVariant = Color(0xFF44474F)

// M3 Fallback Light Colors
val LightPrimary = Color(0xFF0061A4)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD1E4FF)
val LightOnPrimaryContainer = Color(0xFF001D36)

val LightSecondary = Color(0xFF535F70)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD7E3F7)
val LightOnSecondaryContainer = Color(0xFF101C2B)

val LightTertiary = Color(0xFF6B5778)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFF2DAFF)
val LightOnTertiaryContainer = Color(0xFF251431)

val LightBackground = Color(0xFFFDF8FD)
val LightOnBackground = Color(0xFF1A1C1E)
val LightSurface = Color(0xFFFDF8FD)
val LightOnSurface = Color(0xFF1A1C1E)
val LightSurfaceVariant = Color(0xFFDFE2EB)
val LightOnSurfaceVariant = Color(0xFF43474E)
val LightOutline = Color(0xFF73777F)
val LightOutlineVariant = Color(0xFFC3C7D0)

// Static OLED Palette values for fallback configurations
val StaticOledBlack = Color(0xFF000000)
val StaticOledSurface = Color(0xFF0D0E12)
val StaticOledSurfaceVariant = Color(0xFF161820)
val StaticOledSurfaceContainer = Color(0xFF1E202B)
val StaticOledBorder = Color(0xFF2B2E3D)
