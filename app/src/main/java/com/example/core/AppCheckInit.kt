package com.example.core

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val HARNESS_DEBUG_TOKEN_KEY = "aistudio_harness_debug_token_configured"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

internal enum class AppCheckProviderMode {
    DEBUG_HARNESS,
    RECAPTCHA,
    DEBUG_FALLBACK,
    NONE
}

internal fun resolveAppCheckMode(
    isDebugBuild: Boolean,
    intentDebugToken: String?,
    hasPersistedHarnessToken: Boolean,
    recaptchaSiteKey: String
): AppCheckProviderMode {
    if (isDebugBuild && (!intentDebugToken.isNullOrEmpty() || hasPersistedHarnessToken)) {
        return AppCheckProviderMode.DEBUG_HARNESS
    }
    if (recaptchaSiteKey.isNotBlank()) {
        return AppCheckProviderMode.RECAPTCHA
    }
    return if (isDebugBuild) AppCheckProviderMode.DEBUG_FALLBACK else AppCheckProviderMode.NONE
}

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        val prefs = context.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )

        val debugToken = if (BuildConfig.DEBUG) intent?.getStringExtra(DEBUG_TOKEN_EXTRA) else null
        if (BuildConfig.DEBUG && !debugToken.isNullOrEmpty()) {
            // Synchronous commit() is required before DebugAppCheckProviderFactory reads it.
            prefs.edit()
                .putString(DEBUG_SECRET_KEY, debugToken)
                .putBoolean(HARNESS_DEBUG_TOKEN_KEY, true)
                .commit()
            Log.d(TAG, "App Check debug secret configured successfully.")
        }

        val hasPersistedHarnessToken = BuildConfig.DEBUG &&
            prefs.getBoolean(HARNESS_DEBUG_TOKEN_KEY, false) &&
            !prefs.getString(DEBUG_SECRET_KEY, null).isNullOrBlank()

        val resId = context.resources.getIdentifier(
            RECAPTCHA_SITE_KEY_RES,
            "string",
            context.packageName
        )
        val siteKey = if (resId != 0) context.getString(resId).trim() else ""

        when (
            resolveAppCheckMode(
                isDebugBuild = BuildConfig.DEBUG,
                intentDebugToken = debugToken,
                hasPersistedHarnessToken = hasPersistedHarnessToken,
                recaptchaSiteKey = siteKey
            )
        ) {
            AppCheckProviderMode.DEBUG_HARNESS -> {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "DebugAppCheckProviderFactory installed.")
            }
            AppCheckProviderMode.RECAPTCHA -> {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                )
                Log.d(TAG, "RecaptchaAppCheckProviderFactory installed (debug=${BuildConfig.DEBUG}).")
            }
            AppCheckProviderMode.DEBUG_FALLBACK -> {
                Log.w(TAG, "No debug token or recaptcha_site_key found; falling back to DebugAppCheckProviderFactory.")
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            }
            AppCheckProviderMode.NONE -> {
                Log.e(TAG, "recaptcha_site_key string resource not found or blank in package.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}
