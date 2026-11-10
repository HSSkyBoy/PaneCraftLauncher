package com.tungsten.fcl.pclf.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * Authentic PCL2 x Miuix color tokens derived dynamically from FCL's [ThemeData] accent.
 */
@Immutable
data class PclfColors(
    val accent: Color,
    val accentLight: Color,
    val accentDark: Color,
    val titleBarStart: Color,
    val titleBarEnd: Color,
    val canvasTint: Color,
    val sidebarBg: Color,
    val sidebarBorder: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val buttonBg: Color,
    val buttonBorderNeutral: Color,
    val buttonBorderAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean
)

val LocalPclfColors = staticCompositionLocalOf {
    buildPclfColors(seedColorInt = 0xFF1576D8.toInt(), isDark = false)
}

fun buildPclfColors(seedColorInt: Int, isDark: Boolean): PclfColors {
    val opaqueSeed = seedColorInt or 0xFF000000.toInt()
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(opaqueSeed, hsl)

    val startHsl = hsl.copyOf().apply {
        this[2] = (this[2] * 0.92f).coerceIn(0.22f, 0.52f)
    }
    val endHsl = hsl.copyOf().apply {
        this[2] = (this[2] * 1.06f).coerceIn(0.28f, 0.58f)
    }
    val lightHsl = hsl.copyOf().apply {
        this[2] = (this[2] + 0.16f).coerceIn(0.42f, 0.85f)
    }
    val canvasHsl = hsl.copyOf().apply {
        this[1] = (this[1] * 0.55f).coerceIn(0.15f, 0.65f)
        this[2] = if (isDark) 0.13f else 0.89f
    }

    val accent = Color(opaqueSeed)
    val titleBarStart = Color(ColorUtils.HSLToColor(startHsl))
    val titleBarEnd = Color(ColorUtils.HSLToColor(endHsl))
    val accentLight = Color(ColorUtils.HSLToColor(lightHsl))
    val canvasTint = Color(ColorUtils.HSLToColor(canvasHsl))

    return if (isDark) {
        PclfColors(
            accent = accentLight,
            accentLight = accentLight,
            accentDark = titleBarStart,
            titleBarStart = titleBarStart,
            titleBarEnd = titleBarEnd,
            canvasTint = canvasTint,
            sidebarBg = Color(0xF51A1D24),
            sidebarBorder = Color(0x1EFFFFFF),
            cardBg = Color(0xEB222630),
            cardBorder = Color(0x1EFFFFFF),
            buttonBg = Color(0xE6252A34),
            buttonBorderNeutral = Color(0xFF555E70),
            buttonBorderAccent = accentLight,
            textPrimary = Color(0xFFF2F4F8),
            textSecondary = Color(0xFF9AA4B8),
            isDark = true
        )
    } else {
        PclfColors(
            accent = titleBarStart,
            accentLight = accentLight,
            accentDark = titleBarStart,
            titleBarStart = titleBarStart,
            titleBarEnd = titleBarEnd,
            canvasTint = canvasTint,
            sidebarBg = Color(0xFAFFFFFF),
            sidebarBorder = Color(0x12000000),
            cardBg = Color(0xF2FFFFFF),
            cardBorder = Color(0x16000000),
            buttonBg = Color(0xF5FFFFFF),
            buttonBorderNeutral = Color(0xFF8C96A5),
            buttonBorderAccent = titleBarStart,
            textPrimary = Color(0xFF1F242D),
            textSecondary = Color(0xFF7B8494),
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
