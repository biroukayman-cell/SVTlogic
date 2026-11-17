package com.example.core

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object SecurityLockManager {
    private const val PREFS_NAME = "bioterre_secondary_security"
    private const val KEY_PREFIX_HASH = "pin_hash_"
    private const val KEY_PREFIX_SALT = "pin_salt_"
    private const val KEY_PREFIX_FAILS = "pin_fails_"
    private const val KEY_PREFIX_LOCKOUT_UNTIL = "pin_lockout_until_"

    const val MIN_PIN_LENGTH = 4
    const val MAX_PIN_LENGTH = 6
    const val MAX_FAILED_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 30_000L

    private val secureRandom = SecureRandom()

    fun isValidPinFormat(pin: String): Boolean {
        return pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it.isDigit() }
    }

    fun isPinEnabled(context: Context, userId: String): Boolean {
        if (userId.isBlank()) return false
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hash = prefs.getString("$KEY_PREFIX_HASH$userId", null)
        val salt = prefs.getString("$KEY_PREFIX_SALT$userId", null)
        return !hash.isNullOrBlank() && !salt.isNullOrBlank()
    }

    fun setPin(context: Context, userId: String, pin: String): Boolean {
        if (userId.isBlank() || !isValidPinFormat(pin)) return false
        val saltBytes = ByteArray(16)
        secureRandom.nextBytes(saltBytes)
        val saltB64 = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        val hashB64 = hashPinWithSalt(pin, saltB64)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("$KEY_PREFIX_SALT$userId", saltB64)
            .putString("$KEY_PREFIX_HASH$userId", hashB64)
            .putInt("$KEY_PREFIX_FAILS$userId", 0)
            .putLong("$KEY_PREFIX_LOCKOUT_UNTIL$userId", 0L)
            .apply()
        return true
    }

    fun verifyPin(
        context: Context,
        userId: String,
        candidatePin: String,
        nowMillis: Long = System.currentTimeMillis()
    ): PinVerifyResult {
        if (userId.isBlank() || !isPinEnabled(context, userId)) {
            return PinVerifyResult.NotConfigured
        }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lockoutUntil = prefs.getLong("$KEY_PREFIX_LOCKOUT_UNTIL$userId", 0L)
        if (lockoutUntil > nowMillis) {
            val remainingSec = ((lockoutUntil - nowMillis + 999L) / 1000L).toInt().coerceAtLeast(1)
            return PinVerifyResult.LockedOut(remainingSec)
        }

        val saltB64 = prefs.getString("$KEY_PREFIX_SALT$userId", null).orEmpty()
        val expectedHash = prefs.getString("$KEY_PREFIX_HASH$userId", null).orEmpty()
        val candidateHash = hashPinWithSalt(candidatePin, saltB64)

        return if (MessageDigest.isEqual(expectedHash.toByteArray(Charsets.UTF_8), candidateHash.toByteArray(Charsets.UTF_8))) {
            prefs.edit()
                .putInt("$KEY_PREFIX_FAILS$userId", 0)
                .putLong("$KEY_PREFIX_LOCKOUT_UNTIL$userId", 0L)
                .apply()
            PinVerifyResult.Success
        } else {
            val fails = prefs.getInt("$KEY_PREFIX_FAILS$userId", 0) + 1
            if (fails >= MAX_FAILED_ATTEMPTS) {
                val until = nowMillis + LOCKOUT_DURATION_MS
                prefs.edit()
                    .putInt("$KEY_PREFIX_FAILS$userId", 0)
                    .putLong("$KEY_PREFIX_LOCKOUT_UNTIL$userId", until)
                    .apply()
                PinVerifyResult.LockedOut((LOCKOUT_DURATION_MS / 1000L).toInt())
            } else {
                prefs.edit()
                    .putInt("$KEY_PREFIX_FAILS$userId", fails)
                    .apply()
                PinVerifyResult.Invalid(attemptsRemaining = MAX_FAILED_ATTEMPTS - fails)
            }
        }
    }

    fun disablePin(context: Context, userId: String) {
        if (userId.isBlank()) return
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove("$KEY_PREFIX_SALT$userId")
            .remove("$KEY_PREFIX_HASH$userId")
            .remove("$KEY_PREFIX_FAILS$userId")
            .remove("$KEY_PREFIX_LOCKOUT_UNTIL$userId")
            .apply()
    }

    fun getRemainingLockoutSeconds(
        context: Context,
        userId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        if (userId.isBlank()) return 0
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lockoutUntil = prefs.getLong("$KEY_PREFIX_LOCKOUT_UNTIL$userId", 0L)
        if (lockoutUntil <= nowMillis) return 0
        return ((lockoutUntil - nowMillis + 999L) / 1000L).toInt().coerceAtLeast(1)
    }

    internal fun hashPinWithSalt(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin:bioterre_svt".toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

sealed interface PinVerifyResult {
    data object Success : PinVerifyResult
    data object NotConfigured : PinVerifyResult
    data class Invalid(val attemptsRemaining: Int) : PinVerifyResult
    data class LockedOut(val remainingSeconds: Int) : PinVerifyResult
}
