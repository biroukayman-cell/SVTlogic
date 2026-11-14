package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.core.AppThemePalette
import com.example.core.ThemeConfig
import com.example.core.ThemeMode

private val EmeraldLightColorScheme = lightColorScheme(
    primary = SvtEmeraldPrimary,
    onPrimary = SvtOnEmeraldPrimary,
    primaryContainer = SvtEmeraldContainer,
    onPrimaryContainer = SvtOnEmeraldContainer,
    secondary = SvtOceanSecondary,
    onSecondary = SvtOnOceanSecondary,
    secondaryContainer = SvtOceanContainer,
    onSecondaryContainer = SvtOnOceanContainer,
    tertiary = SvtGoldTertiary,
    onTertiary = SvtOnGoldTertiary,
    tertiaryContainer = SvtGoldContainer,
    onTertiaryContainer = SvtOnGoldContainer,
    background = SvtLightBackground,
    onBackground = SvtLightOnSurface,
    surface = SvtLightSurface,
    onSurface = SvtLightOnSurface,
    surfaceVariant = SvtLightSurfaceVariant,
    onSurfaceVariant = SvtLightOnSurfaceVariant,
    outline = SvtLightOutline,
    error = SvtCoralError
)

private val EmeraldDarkColorScheme = darkColorScheme(
    primary = SvtDarkPrimary,
    onPrimary = SvtDarkOnPrimary,
    primaryContainer = SvtDarkPrimaryContainer,
    onPrimaryContainer = SvtDarkOnPrimaryContainer,
    secondary = SvtDarkSecondary,
    onSecondary = SvtDarkOnSecondary,
    secondaryContainer = SvtDarkSecondaryContainer,
    onSecondaryContainer = SvtDarkOnSecondaryContainer,
    tertiary = SvtDarkTertiary,
    onTertiary = SvtDarkOnTertiary,
    tertiaryContainer = SvtDarkTertiaryContainer,
    onTertiaryContainer = SvtDarkOnTertiaryContainer,
    background = SvtDarkBackground,
    onBackground = SvtDarkOnSurface,
    surface = SvtDarkSurface,
    onSurface = SvtDarkOnSurface,
    surfaceVariant = SvtDarkSurfaceVariant,
    onSurfaceVariant = SvtDarkOnSurfaceVariant,
    outline = SvtDarkOutline,
    error = SvtCoralError
)

private val OceanLightColorScheme = lightColorScheme(
    primary = OceanPrimary,
    onPrimary = OceanOnPrimary,
    primaryContainer = OceanPrimaryContainer,
    onPrimaryContainer = OceanOnPrimaryContainer,
    secondary = OceanSecondary,
    onSecondary = OceanOnSecondary,
    secondaryContainer = OceanSecondaryContainer,
    onSecondaryContainer = OceanOnSecondaryContainer,
    tertiary = SvtGoldTertiary,
    onTertiary = SvtOnGoldTertiary,
    tertiaryContainer = SvtGoldContainer,
    onTertiaryContainer = SvtOnGoldContainer,
    background = OceanLightBg,
    onBackground = Color(0xFF101D29),
    surface = Color.White,
    onSurface = Color(0xFF101D29),
    surfaceVariant = OceanLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF3E4E5E),
    outline = Color(0xFF6F8294),
    error = SvtCoralError
)

private val OceanDarkColorScheme = darkColorScheme(
    primary = OceanDarkPrimary,
    onPrimary = OceanDarkOnPrimary,
    primaryContainer = OceanDarkPrimaryContainer,
    onPrimaryContainer = OceanDarkOnPrimaryContainer,
    secondary = SvtDarkPrimary,
    onSecondary = SvtDarkOnPrimary,
    secondaryContainer = SvtDarkPrimaryContainer,
    onSecondaryContainer = SvtDarkOnPrimaryContainer,
    tertiary = SvtDarkTertiary,
    onTertiary = SvtDarkOnTertiary,
    tertiaryContainer = SvtDarkTertiaryContainer,
    onTertiaryContainer = SvtDarkOnTertiaryContainer,
    background = OceanDarkBg,
    onBackground = Color(0xFFDCE6F2),
    surface = OceanDarkSurface,
    onSurface = Color(0xFFDCE6F2),
    surfaceVariant = OceanDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA7B8C9),
    outline = Color(0xFF6E8296),
    error = SvtCoralError
)

private val EarthLightColorScheme = lightColorScheme(
    primary = EarthPrimary,
    onPrimary = EarthOnPrimary,
    primaryContainer = EarthPrimaryContainer,
    onPrimaryContainer = EarthOnPrimaryContainer,
    secondary = EarthSecondary,
    onSecondary = EarthOnSecondary,
    secondaryContainer = EarthSecondaryContainer,
    onSecondaryContainer = EarthOnSecondaryContainer,
    tertiary = SvtOceanSecondary,
    onTertiary = SvtOnOceanSecondary,
    tertiaryContainer = SvtOceanContainer,
    onTertiaryContainer = SvtOnOceanContainer,
    background = EarthLightBg,
    onBackground = Color(0xFF231914),
    surface = Color.White,
    onSurface = Color(0xFF231914),
    surfaceVariant = EarthLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF56443B),
    outline = Color(0xFF877368),
    error = SvtCoralError
)

private val EarthDarkColorScheme = darkColorScheme(
    primary = EarthDarkPrimary,
    onPrimary = EarthDarkOnPrimary,
    primaryContainer = EarthDarkPrimaryContainer,
    onPrimaryContainer = EarthDarkOnPrimaryContainer,
    secondary = Color(0xFFC4D68D),
    onSecondary = Color(0xFF2C3409),
    secondaryContainer = Color(0xFF424D19),
    onSecondaryContainer = Color(0xFFE1F1A8),
    tertiary = SvtDarkSecondary,
    onTertiary = SvtDarkOnSecondary,
    tertiaryContainer = SvtDarkSecondaryContainer,
    onTertiaryContainer = SvtDarkOnSecondaryContainer,
    background = EarthDarkBg,
    onBackground = Color(0xFFF0DFD7),
    surface = EarthDarkSurface,
    onSurface = Color(0xFFF0DFD7),
    surfaceVariant = EarthDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBB8AE),
    outline = Color(0xFF938077),
    error = SvtCoralError
)

private val BotanicalLightColorScheme = lightColorScheme(
    primary = BotanicalPrimary,
    onPrimary = BotanicalOnPrimary,
    primaryContainer = BotanicalPrimaryContainer,
    onPrimaryContainer = BotanicalOnPrimaryContainer,
    secondary = BotanicalSecondary,
    onSecondary = BotanicalOnSecondary,
    secondaryContainer = BotanicalSecondaryContainer,
    onSecondaryContainer = BotanicalOnSecondaryContainer,
    tertiary = SvtGoldTertiary,
    onTertiary = SvtOnGoldTertiary,
    tertiaryContainer = SvtGoldContainer,
    onTertiaryContainer = SvtOnGoldContainer,
    background = BotanicalLightBg,
    onBackground = Color(0xFF1D1628),
    surface = Color.White,
    onSurface = Color(0xFF1D1628),
    surfaceVariant = BotanicalLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF4D425E),
    outline = Color(0xFF7D708E),
    error = SvtCoralError
)

private val BotanicalDarkColorScheme = darkColorScheme(
    primary = BotanicalDarkPrimary,
    onPrimary = BotanicalDarkOnPrimary,
    primaryContainer = BotanicalDarkPrimaryContainer,
    onPrimaryContainer = BotanicalDarkOnPrimaryContainer,
    secondary = SvtDarkPrimary,
    onSecondary = SvtDarkOnPrimary,
    secondaryContainer = SvtDarkPrimaryContainer,
    onSecondaryContainer = SvtDarkOnPrimaryContainer,
    tertiary = SvtDarkTertiary,
    onTertiary = SvtDarkOnTertiary,
    tertiaryContainer = SvtDarkTertiaryContainer,
    onTertiaryContainer = SvtDarkOnTertiaryContainer,
    background = BotanicalDarkBg,
    onBackground = Color(0xFFE9DFF5),
    surface = BotanicalDarkSurface,
    onSurface = Color(0xFFE9DFF5),
    surfaceVariant = BotanicalDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC2B5D3),
    outline = Color(0xFF8C7E9E),
    error = SvtCoralError
)

fun AppThemePalette.previewSwatchColors(): Pair<Color, Color> = when (this) {
    AppThemePalette.EMERALD -> SvtEmeraldPrimary to SvtOceanSecondary
    AppThemePalette.OCEAN -> OceanPrimary to OceanSecondary
    AppThemePalette.EARTH -> EarthPrimary to EarthSecondary
    AppThemePalette.BOTANICAL -> BotanicalPrimary to BotanicalSecondary
}

private fun resolveColorScheme(palette: AppThemePalette, isDark: Boolean): ColorScheme {
    return when (palette) {
        AppThemePalette.EMERALD -> if (isDark) EmeraldDarkColorScheme else EmeraldLightColorScheme
        AppThemePalette.OCEAN -> if (isDark) OceanDarkColorScheme else OceanLightColorScheme
        AppThemePalette.EARTH -> if (isDark) EarthDarkColorScheme else EarthLightColorScheme
        AppThemePalette.BOTANICAL -> if (isDark) BotanicalDarkColorScheme else BotanicalLightColorScheme
    }
}

val SvtShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BioTerreTheme(
    themeConfig: ThemeConfig = ThemeConfig(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeConfig.mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        resolveColorScheme(themeConfig.palette, darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SvtShapes,
        content = content
    )
}
