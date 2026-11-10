package com.tungsten.fcl.pclf.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * PCLF (PCL2 x Miuix) color tokens derived dynamically from FCL's [ThemeData] accent.
 */
@Immutable
data class PclfColors(
    val accent: Color,
    val accentLight: Color,
    val accentDark: Color,
    val titleBarStart: Color,
    val titleBarEnd: Color,
    val sidebarBg: Color,
    val sidebarBorder: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val buttonBg: Color,
    val buttonBorder: Color,
    val launchFillStart: Color,
    val launchFillEnd: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean
)

val LocalPclfColors = staticCompositionLocalOf {
    buildPclfColors(seedColorInt = 0xFF1E78D6.toInt(), isDark = false)
}

fun buildPclfColors(seedColorInt: Int, isDark: Boolean): PclfColors {
    val opaqueSeed = seedColorInt or 0xFF000000.toInt()
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(opaqueSeed, hsl)

    // Classic PCL gradient across Title Bar & Launch Button
    val startHsl = hsl.copyOf().apply {
        this[2] = (this[2] * 0.88f).coerceIn(0.18f, 0.72f)
    }
    val endHsl = hsl.copyOf().apply {
        this[2] = (this[2] * 1.12f).coerceIn(0.28f, 0.82f)
    }
    val lightHsl = hsl.copyOf().apply {
        this[2] = (this[2] + 0.16f).coerceIn(0.35f, 0.92f)
    }

    val accent = Color(opaqueSeed)
    val titleBarStart = Color(ColorUtils.HSLToColor(startHsl))
    val titleBarEnd = Color(ColorUtils.HSLToColor(endHsl))
    val accentLight = Color(ColorUtils.HSLToColor(lightHsl))

    return if (isDark) {
        PclfColors(
            accent = accentLight,
            accentLight = accentLight,
            accentDark = titleBarStart,
            titleBarStart = titleBarStart,
            titleBarEnd = accent,
            sidebarBg = Color(0xD91A1D24),
            sidebarBorder = Color(0x1FFFFFFF),
            cardBg = Color(0xCC222630),
            cardBorder = Color(0x1EFFFFFF),
            buttonBg = Color(0xCC2A2F3A),
            buttonBorder = accentLight.copy(alpha = 0.75f),
            launchFillStart = titleBarStart,
            launchFillEnd = titleBarEnd,
            textPrimary = Color(0xFFF2F4F8),
            textSecondary = Color(0xFFA6B0C2),
            isDark = true
        )
    } else {
        PclfColors(
            accent = accent,
            accentLight = accentLight,
            accentDark = titleBarStart,
            titleBarStart = titleBarStart,
            titleBarEnd = titleBarEnd,
            sidebarBg = Color(0xE0F6F9FD),
            sidebarBorder = Color(0x18000000),
            cardBg = Color(0xD9FFFFFF),
            cardBorder = Color(0x14000000),
            buttonBg = Color(0xE6FFFFFF),
            buttonBorder = accent.copy(alpha = 0.82f),
            launchFillStart = titleBarStart,
            launchFillEnd = titleBarEnd,
            textPrimary = Color(0xFF1D232E),
            textSecondary = Color(0xFF5D6778),
            isDark = false
        )
    }
}

object PclfTheme {
    val colors: PclfColors
        @Composable @ReadOnlyComposable
        get() = LocalPclfColors.current
}

@Composable
fun PclfTheme(
    seedColorInt: Int,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val pclfColors = remember(seedColorInt, darkTheme) {
        buildPclfColors(seedColorInt, darkTheme)
    }
    val controller = remember(seedColorInt, darkTheme) {
        ThemeController(
            colorSchemeMode = if (darkTheme) ColorSchemeMode.Dark else ColorSchemeMode.Light,
            keyColor = Color(seedColorInt or 0xFF000000.toInt())
        )
    }
    MiuixTheme(controller = controller) {
        CompositionLocalProvider(LocalPclfColors provides pclfColors) {
            content()
        }
    }
}
