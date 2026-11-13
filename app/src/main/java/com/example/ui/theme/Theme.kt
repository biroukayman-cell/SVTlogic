package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
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

private val DarkColorScheme = darkColorScheme(
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

val SvtShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BioTerreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SvtShapes,
        content = content
    )
}
