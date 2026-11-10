package com.example.data

import android.util.Log
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

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?,
    auth: FirebaseAuth = FirebaseAuth.getInstance()
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = try {
        auth.currentUser
    } catch (_: Exception) {
        null
    }

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

fun toHindiErrorMessage(error: Throwable): String {
    val causes = generateSequence(error) { it.cause }
    val firestoreError = causes.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    return when (firestoreError?.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Anumati nahi mili. Kripya dubara login karke koshish karein."
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Internet connection kamzor hai ya server band hai. Kripya thodi der baad koshish karein."
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
            "Aaj ki free limit poori ho gayi hai. Kal dubara koshish karein."
        FirebaseFirestoreException.Code.NOT_FOUND ->
            "Yeh jankari ab uplabdh nahi hai."
        FirebaseFirestoreException.Code.UNAUTHENTICATED ->
            "Kripya pehle Google se Login karein."
        else -> "Kuch gadbad ho gayi hai. Kripya apna internet check karein aur dubara koshish karein."
    }
}
