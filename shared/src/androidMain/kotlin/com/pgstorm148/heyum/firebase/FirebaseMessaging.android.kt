package com.pgstorm148.heyum.firebase

import com.google.firebase.messaging.FirebaseMessaging as AndroidFCM
import kotlinx.coroutines.tasks.await

actual class FirebaseMessaging private constructor() {
    actual companion object {
        private val instance = FirebaseMessaging()
        actual fun getInstance(): FirebaseMessaging = instance
    }

    actual suspend fun getToken(): String? {
        return try {
            AndroidFCM.getInstance().token.await()
        } catch (e: Exception) {
            null
        }
    }

    actual suspend fun subscribeToTopic(topic: String) {
        try {
            AndroidFCM.getInstance().subscribeToTopic(topic).await()
        } catch (e: Exception) {
            // Log or ignore
        }
    }

    actual suspend fun unsubscribeFromTopic(topic: String) {
        try {
            AndroidFCM.getInstance().unsubscribeFromTopic(topic).await()
        } catch (e: Exception) {
            // Log or ignore
        }
    }

    actual fun setBadgeCount(count: Int) {
        // No-op on Android (standard Android handles badges automatically via notification dots)
    }
}
