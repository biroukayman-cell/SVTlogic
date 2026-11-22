package com.example.core

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class BiometricAvailability {
    AVAILABLE,
    NONE_ENROLLED,
    HARDWARE_UNAVAILABLE,
    NO_HARDWARE
}

data class SavedBiometricSummary(
    val userId: String,
    val role: String,
    val displayName: String,
    val identifierLabel: String
)

data class SavedBiometricCredential(
    val userId: String,
    val role: String,
    val displayName: String,
    val identifierLabel: String,
    val email: String,
    val password: String
)

object BiometricAuthHelper {
    private const val TAG = "BiometricAuthHelper"
    private const val PREFS_NAME = "bioterre_biometric_auth"
    private const val KEY_CIPHERTEXT = "cred_ciphertext"
    private const val KEY_IV = "cred_iv"
    private const val KEY_USER_ID = "cred_user_id"
    private const val KEY_ROLE = "cred_role"
    private const val KEY_DISPLAY_NAME = "cred_display_name"
    private const val KEY_IDENTIFIER_LABEL = "cred_identifier_label"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "bioterre_biometric_cred_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128

    private const val ALLOWED_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun getAvailability(context: Context): BiometricAvailability {
        return try {
            val manager = BiometricManager.from(context.applicationContext)
            when (manager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
                else -> BiometricAvailability.NO_HARDWARE
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query biometric availability", e)
            BiometricAvailability.NO_HARDWARE
        }
    }

    fun hasBiometricHardware(availability: BiometricAvailability): Boolean {
        return availability == BiometricAvailability.AVAILABLE ||
            availability == BiometricAvailability.NONE_ENROLLED ||
            availability == BiometricAvailability.HARDWARE_UNAVAILABLE
    }

    fun canShowFingerprintLogin(
        availability: BiometricAvailability,
        hasSavedCredential: Boolean
    ): Boolean {
        return availability == BiometricAvailability.AVAILABLE && hasSavedCredential
    }

    fun hasSavedCredential(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cipher = prefs.getString(KEY_CIPHERTEXT, null)
        val iv = prefs.getString(KEY_IV, null)
        val userId = prefs.getString(KEY_USER_ID, null)
        return !cipher.isNullOrBlank() && !iv.isNullOrBlank() && !userId.isNullOrBlank()
    }

    fun getSavedSummary(context: Context): SavedBiometricSummary? {
        if (!hasSavedCredential(context)) return null
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userId = prefs.getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() } ?: return null
        return SavedBiometricSummary(
            userId = userId,
            role = prefs.getString(KEY_ROLE, "student").orEmpty(),
            displayName = prefs.getString(KEY_DISPLAY_NAME, "").orEmpty(),
            identifierLabel = prefs.getString(KEY_IDENTIFIER_LABEL, "").orEmpty()
        )
    }

    fun isBiometricEnabledForUser(context: Context, userId: String): Boolean {
        if (userId.isBlank()) return false
        return getSavedSummary(context)?.userId == userId
    }

    fun saveCredential(context: Context, credential: SavedBiometricCredential): Boolean {
        if (credential.userId.isBlank() || credential.email.isBlank() || credential.password.isBlank()) {
            return false
        }
        return try {
            val payload = encodeCredentialJson(credential).toByteArray(Charsets.UTF_8)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val encrypted = cipher.doFinal(payload)
            val iv = cipher.iv

            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .putString(KEY_USER_ID, credential.userId)
                .putString(KEY_ROLE, credential.role)
                .putString(KEY_DISPLAY_NAME, credential.displayName)
                .putString(KEY_IDENTIFIER_LABEL, credential.identifierLabel)
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt and save biometric credential", e)
            false
        }
    }

    fun decryptSavedCredential(context: Context): SavedBiometricCredential? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cipherB64 = prefs.getString(KEY_CIPHERTEXT, null) ?: return null
        val ivB64 = prefs.getString(KEY_IV, null) ?: return null
        return try {
            val encrypted = Base64.decode(cipherB64, Base64.NO_WRAP)
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            val key = getExistingSecretKey() ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val plain = cipher.doFinal(encrypted).toString(Charsets.UTF_8)
            decodeCredentialJson(plain)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt biometric credential", e)
            null
        }
    }

    fun updatePasswordIfSaved(context: Context, userId: String, newPassword: String) {
        if (!isBiometricEnabledForUser(context, userId) || newPassword.isBlank()) return
        val current = decryptSavedCredential(context) ?: return
        if (current.userId == userId) {
            saveCredential(context, current.copy(password = newPassword))
        }
    }

    fun updateDisplayNameIfSaved(context: Context, userId: String, displayName: String) {
        if (!isBiometricEnabledForUser(context, userId) || displayName.isBlank()) return
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DISPLAY_NAME, displayName)
            .apply()
    }

    fun clearSavedCredential(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_CIPHERTEXT)
            .remove(KEY_IV)
            .remove(KEY_USER_ID)
            .remove(KEY_ROLE)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_IDENTIFIER_LABEL)
            .commit()
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (String?) -> Unit
    ) {
        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_CANCELED
                        ) {
                            onError(null)
                        } else {
                            onError(errString.toString())
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        // System prompt remains open and displays built-in retry feedback.
                    }
                }
            )
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .setConfirmationRequired(false)
                .build()
            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch BiometricPrompt", e)
            onError(e.localizedMessage)
        }
    }

    fun buildEnrollIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, ALLOWED_AUTHENTICATORS)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    internal fun encodeCredentialJson(credential: SavedBiometricCredential): String {
        return JSONObject()
            .put("userId", credential.userId)
            .put("role", credential.role)
            .put("displayName", credential.displayName)
            .put("identifierLabel", credential.identifierLabel)
            .put("email", credential.email)
            .put("password", credential.password)
            .toString()
    }

    internal fun decodeCredentialJson(json: String): SavedBiometricCredential? {
        return try {
            val obj = JSONObject(json)
            val userId = obj.optString("userId", "").trim()
            val email = obj.optString("email", "").trim()
            val password = obj.optString("password", "")
            if (userId.isBlank() || email.isBlank() || password.isBlank()) {
                null
            } else {
                SavedBiometricCredential(
                    userId = userId,
                    role = obj.optString("role", "student"),
                    displayName = obj.optString("displayName", ""),
                    identifierLabel = obj.optString("identifierLabel", email),
                    email = email,
                    password = password
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getExistingSecretKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    }

    private fun getOrCreateSecretKey(): SecretKey {
        getExistingSecretKey()?.let { return it }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}

tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
