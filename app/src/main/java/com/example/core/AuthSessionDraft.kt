package com.example.core

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import java.security.SecureRandom
import java.util.Locale

data class PendingAuthDraft(
    val role: String = "student",
    val massarNumber: String = "",
    val email: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val establishment: String = "",
    val pendingClassQrToken: String = ""
)

object AuthSessionDraft {
    private const val PREFS = "bioterre_auth_draft"
    private const val KEY_ROLE = "role"
    private const val KEY_MASSAR = "massar"
    private const val KEY_EMAIL = "email"
    private const val KEY_FULL_NAME = "full_name"
    private const val KEY_PHONE = "phone_number"
    private const val KEY_ESTABLISHMENT = "establishment"
    private const val KEY_QR_TOKEN = "pending_qr_token"

    private const val PASS_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789"
    private val random = SecureRandom()

    fun save(
        context: Context,
        role: String,
        massarNumber: String = "",
        email: String = "",
        fullName: String = "",
        phoneNumber: String = "",
        establishment: String = "",
        pendingClassQrToken: String = ""
    ) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ROLE, role)
            .putString(KEY_MASSAR, normalizeMassar(massarNumber))
            .putString(KEY_EMAIL, email.trim())
            .putString(KEY_FULL_NAME, fullName.trim())
            .putString(KEY_PHONE, phoneNumber.trim())
            .putString(KEY_ESTABLISHMENT, establishment.trim())
            .putString(KEY_QR_TOKEN, pendingClassQrToken.trim())
            .apply()
    }

    fun savePendingClassQrToken(context: Context, qrToken: String) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_QR_TOKEN, qrToken.trim())
            .apply()
    }

    fun load(context: Context): PendingAuthDraft {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return PendingAuthDraft(
            role = prefs.getString(KEY_ROLE, "student") ?: "student",
            massarNumber = prefs.getString(KEY_MASSAR, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            fullName = prefs.getString(KEY_FULL_NAME, "") ?: "",
            phoneNumber = prefs.getString(KEY_PHONE, "") ?: "",
            establishment = prefs.getString(KEY_ESTABLISHMENT, "") ?: "",
            pendingClassQrToken = prefs.getString(KEY_QR_TOKEN, "") ?: ""
        )
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun normalizeMassar(raw: String): String {
        return raw.trim()
            .uppercase(Locale.US)
            .replace(Regex("[^A-Z0-9_-]"), "")
    }

    fun massarToAuthEmail(massar: String): String {
        val clean = normalizeMassar(massar).lowercase(Locale.US)
        return "massar.$clean@student.bioterre.svt"
    }

    fun defaultStudentTaalimEmail(massar: String): String {
        val clean = normalizeMassar(massar)
        if (clean.isBlank() || clean.startsWith("QR")) return ""
        return "${clean.lowercase(Locale.US)}@taalim.ma"
    }

    fun generateProvisionalPassword(length: Int = 8): String {
        val sb = StringBuilder("Svt")
        val remaining = (length - 3).coerceAtLeast(5)
        repeat(remaining) {
            sb.append(PASS_CHARS[random.nextInt(PASS_CHARS.length)])
        }
        return sb.toString()
    }

    fun getProvisionerAuth(context: Context): FirebaseAuth {
        val appName = "bioterre_student_provisioner"
        val appContext = context.applicationContext
        val existing = FirebaseApp.getApps(appContext).firstOrNull { it.name == appName }
        val secondaryApp = existing ?: FirebaseApp.initializeApp(
            appContext,
            FirebaseApp.getInstance().options,
            appName
        )
        installAppCheckForSecondaryApp(appContext, secondaryApp)
        return FirebaseAuth.getInstance(secondaryApp)
    }
}
