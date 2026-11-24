package com.example.core

import android.util.Log
import com.example.BuildConfig
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
