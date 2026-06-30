package com.pgstorm148.heyum.firebase

expect class FirebaseMessaging {
    companion object {
        fun getInstance(): FirebaseMessaging
    }
    suspend fun getToken(): String?
    suspend fun subscribeToTopic(topic: String)
    suspend fun unsubscribeFromTopic(topic: String)
    fun setBadgeCount(count: Int)
}
