package com.example.core

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityLockManager {
    private const val PREFS_NAME = "bioterre_secondary_security"
    private const val KEY_PREFIX_HASH = "pin_hash_"
    private const val KEY_PREFIX_SALT = "pin_salt_"
    private const val KEY_PREFIX_FAILS = "pin_fails_"
    private const val KEY_PREFIX_LOCKOUT_UNTIL = "pin_lockout_until_"
    private const val HASH_V2_PREFIX = "v2$"
    private const val PBKDF2_ITERATIONS = 100_000
    private const val PBKDF2_KEY_BITS = 256

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
        val hashV2 = hashPinV2(pin, saltB64)
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("$KEY_PREFIX_SALT$userId", saltB64)
            .putString("$KEY_PREFIX_HASH$userId", hashV2)
            .putInt("$KEY_PREFIX_FAILS$userId", 0)
            .putLong("$KEY_PREFIX_LOCKOUT_UNTIL$userId", 0L)
            .commit()
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
        val isV2 = expectedHash.startsWith(HASH_V2_PREFIX)
        val candidateHash = if (isV2) {
            hashPinV2(candidatePin, saltB64)
        } else {
            hashPinWithSalt(candidatePin, saltB64)
        }

        return if (MessageDigest.isEqual(expectedHash.toByteArray(Charsets.UTF_8), candidateHash.toByteArray(Charsets.UTF_8))) {
            val editor = prefs.edit()
                .putInt("$KEY_PREFIX_FAILS$userId", 0)
                .putLong("$KEY_PREFIX_LOCKOUT_UNTIL$userId", 0L)
            if (!isV2) {
                editor.putString("$KEY_PREFIX_HASH$userId", hashPinV2(candidatePin, saltB64))
            }
            editor.commit()
            PinVerifyResult.Success
        } else {
            val fails = prefs.getInt("$KEY_PREFIX_FAILS$userId", 0) + 1
            if (fails >= MAX_FAILED_ATTEMPTS) {
                val until = nowMillis + LOCKOUT_DURATION_MS
                prefs.edit()
                    .putInt("$KEY_PREFIX_FAILS$userId", 0)
                    .putLong("$KEY_PREFIX_LOCKOUT_UNTIL$userId", until)
                    .commit()
                PinVerifyResult.LockedOut((LOCKOUT_DURATION_MS / 1000L).toInt())
            } else {
                prefs.edit()
                    .putInt("$KEY_PREFIX_FAILS$userId", fails)
                    .commit()
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
            .commit()
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

    internal fun hashPinV2(pin: String, salt: String): String {
        val saltBytes = "$salt:bioterre_svt".toByteArray(Charsets.UTF_8)
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val derived = factory.generateSecret(spec).encoded
            HASH_V2_PREFIX + Base64.encodeToString(derived, Base64.NO_WRAP)
        } finally {
            spec.clearPassword()
        }
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
