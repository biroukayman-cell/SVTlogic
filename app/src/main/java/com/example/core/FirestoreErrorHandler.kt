package com.example.core

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun isQuotaExceededError(error: Throwable?): Boolean {
    if (error == null) return false
    return generateSequence(error) { it.cause }.any { cause ->
        if (cause is FirebaseFirestoreException &&
            cause.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED
        ) {
            return@any true
        }
        val msg = cause.message.orEmpty()
        msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
            msg.contains("Quota exceeded", ignoreCase = true) ||
            msg.contains("free daily", ignoreCase = true) ||
            msg.contains("quota metric", ignoreCase = true) ||
            msg.contains("rate limit", ignoreCase = true) ||
            msg.contains("Too many requests", ignoreCase = true) ||
            msg.contains("Too many attempts", ignoreCase = true) ||
            msg.contains("HTTP 429", ignoreCase = true) ||
            msg.contains("429 Too Many Requests", ignoreCase = true)
    }
}

fun isPermissionDeniedError(error: Throwable?): Boolean {
    if (error == null) return false
    return generateSequence(error) { it.cause }.any { cause ->
        if (cause is FirebaseFirestoreException &&
            cause.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        ) {
            return@any true
        }
        val msg = cause.message.orEmpty()
        msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
            msg.contains("Missing or insufficient permissions", ignoreCase = true)
    }
}

fun userFriendlyFirestoreErrorMessage(
    context: Context,
    error: Throwable,
    fallback: String
): String {
    if (isQuotaExceededError(error)) {
        return context.getString(R.string.quota_exceeded_error)
    }
    if (isPermissionDeniedError(error)) {
        return context.getString(R.string.permission_denied_error)
    }
    return error.localizedMessage?.takeIf { it.isNotBlank() } ?: fallback
}

private fun maskEmailForLogs(email: String?): String? {
    val clean = email?.trim().orEmpty()
    if (clean.isEmpty()) return null
    val atIdx = clean.indexOf('@')
    if (atIdx <= 1) return "***"
    val prefix = clean.substring(0, 2)
    val domain = clean.substring(atIdx)
    return "$prefix***$domain"
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    }

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", maskEmailForLogs(provider.email))
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid?.take(8)?.plus("…"))
        put("email", maskEmailForLogs(currentUser?.email))
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        if (BuildConfig.DEBUG) {
            put("authInfo", authInfoJson)
        }
    }

    val jsonString = errorInfoJson.toString()
    if (BuildConfig.DEBUG) {
        Log.e("FirestoreError", "Firestore Error: $jsonString")
    } else {
        Log.e("FirestoreError", "Firestore ${operationType.value} failed on ${path ?: "unknown"}")
    }
    return jsonString
}
