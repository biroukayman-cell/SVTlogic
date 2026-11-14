package com.example.core

import android.content.Context

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemeMode {
            return entries.firstOrNull { it.key == key } ?: SYSTEM
        }
    }
}

enum class AppThemePalette(val key: String) {
    EMERALD("emerald"),
    OCEAN("ocean"),
    EARTH("earth"),
    BOTANICAL("botanical");

    companion object {
        fun fromKey(key: String?): AppThemePalette {
            return entries.firstOrNull { it.key == key } ?: EMERALD
        }
    }
}

data class ThemeConfig(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val palette: AppThemePalette = AppThemePalette.EMERALD
)

object ThemePreferences {
    private const val PREFS_NAME = "bioterre_svt_prefs"
    private const val KEY_THEME_MODE = "app_theme_mode"
    private const val KEY_THEME_PALETTE = "app_theme_palette"

    fun loadThemeConfig(context: Context): ThemeConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = ThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.key))
        val palette = AppThemePalette.fromKey(prefs.getString(KEY_THEME_PALETTE, AppThemePalette.EMERALD.key))
        return ThemeConfig(mode = mode, palette = palette)
    }

    fun saveThemeConfig(context: Context, config: ThemeConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_THEME_MODE, config.mode.key)
            .putString(KEY_THEME_PALETTE, config.palette.key)
            .apply()
    }
}
