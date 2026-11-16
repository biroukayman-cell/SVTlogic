package com.example.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.data.model.UserProfile
import java.util.UUID

object DeviceSessionManager {

    private const val PREFS_NAME = "bioterre_device_lock"
    private const val KEY_FALLBACK_DEVICE_ID = "fallback_device_id"
    private const val LEGACY_EMULATOR_ANDROID_ID = "9774d56d682e549c"
    private val ALLOWED_ID_CHARS = Regex("[^a-zA-Z0-9_\\-]")

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()?.trim()

        if (!androidId.isNullOrBlank() && androidId != LEGACY_EMULATOR_ANDROID_ID) {
            val cleaned = androidId.replace(ALLOWED_ID_CHARS, "").take(96)
            if (cleaned.isNotBlank()) {
                return "aid_$cleaned"
            }
        }

        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_FALLBACK_DEVICE_ID, null)?.trim()
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val generated = "dev_${UUID.randomUUID().toString().replace("-", "")}"
        prefs.edit().putString(KEY_FALLBACK_DEVICE_ID, generated).apply()
        return generated
    }

    fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        val combined = when {
            manufacturer.isBlank() && model.isBlank() -> "Android"
            model.startsWith(manufacturer, ignoreCase = true) -> model
            manufacturer.isBlank() -> model
            else -> "$manufacturer $model"
        }
        return combined.take(96)
    }

    fun isDeviceAllowedForStudent(profile: UserProfile, currentDeviceId: String): Boolean {
        if (profile.role != "student") return true
        val boundId = profile.boundDeviceId.trim()
        if (boundId.isBlank()) return true
        return boundId == currentDeviceId.trim()
    }
}
