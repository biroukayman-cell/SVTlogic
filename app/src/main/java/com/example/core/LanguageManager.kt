package com.example.core

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

enum class AppLanguage(
    val code: String,
    val nativeLabel: String,
    val layoutDirection: LayoutDirection
) {
    FR("fr", "FR", LayoutDirection.Ltr),
    AR("ar", "AR", LayoutDirection.Rtl),
    EN("en", "EN", LayoutDirection.Ltr);

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code == code } ?: FR
        }
    }
}

object LanguagePreferences {
    private const val PREFS_NAME = "bioterre_svt_prefs"
    private const val KEY_LANG = "app_language"

    fun loadLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppLanguage.fromCode(prefs.getString(KEY_LANG, AppLanguage.FR.code))
    }

    fun saveLanguage(context: Context, language: AppLanguage) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, language.code).apply()
    }
}

@Composable
fun rememberLocalizedContext(baseContext: Context, language: AppLanguage): Context {
    return remember(baseContext, language) {
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        baseContext.createConfigurationContext(config)
    }
}
