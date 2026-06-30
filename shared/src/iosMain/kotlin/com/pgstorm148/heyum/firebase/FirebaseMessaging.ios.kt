package com.pgstorm148.heyum.firebase

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface IosMessagingDelegate {
    fun getToken(completion: (String?, String?) -> Unit)
    fun subscribeToTopic(topic: String, completion: (String?) -> Unit)
    fun unsubscribeFromTopic(topic: String, completion: (String?) -> Unit)
    fun setBadgeCount(count: Int)
}

actual class FirebaseMessaging private constructor() {
    private val delegate: IosMessagingDelegate
        get() = IosFirebaseBridge.messagingDelegate ?: throw IllegalStateException("IosMessagingDelegate is not initialized")

    actual companion object {
        private val instance = FirebaseMessaging()
        actual fun getInstance(): FirebaseMessaging = instance
    }

    actual suspend fun getToken(): String? = suspendCancellableCoroutine { continuation ->
        delegate.getToken { token, error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error))
            } else {
                continuation.resume(token)
            }
        }
    }

    actual suspend fun subscribeToTopic(topic: String) = suspendCancellableCoroutine<Unit> { continuation ->
        delegate.subscribeToTopic(topic) { error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error))
            } else {
                continuation.resume(Unit)
            }
        }
    }

    actual suspend fun unsubscribeFromTopic(topic: String) = suspendCancellableCoroutine<Unit> { continuation ->
        delegate.unsubscribeFromTopic(topic) { error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error))
            } else {
                continuation.resume(Unit)
            }
        }
    }

    actual fun setBadgeCount(count: Int) {
        delegate.setBadgeCount(count)
    }
}
