package com.pgstorm148.heyum.firebase

import com.google.firebase.auth.FirebaseAuth as AndroidAuth
import com.google.firebase.firestore.FirebaseFirestore as AndroidFirestore
import com.google.firebase.firestore.Query
import com.pgstorm148.heyum.model.FirebaseUser
import com.pgstorm148.heyum.model.Craving
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

actual class FirebaseAuth actual constructor() {
    private val auth = AndroidAuth.getInstance()

    actual val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = AndroidAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser?.let { FirebaseUser(it.uid, it.email ?: it.phoneNumber) }
            trySend(user)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    actual val currentCachedUser: FirebaseUser?
        get() = auth.currentUser?.let { FirebaseUser(it.uid, it.email ?: it.phoneNumber) }

    actual suspend fun signIn(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("Sign in returned empty user")
        return FirebaseUser(user.uid, user.email)
    }

    actual suspend fun signUp(email: String, password: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("Sign up returned empty user")
        return FirebaseUser(user.uid, user.email)
    }

    actual fun verifyPhoneNumber(
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onFailed: (String) -> Unit
    ) {
        // Mock fallback for emulator testing, or calls standard verification.
        // We trigger mock verification ID for testing without SMS fees.
        onCodeSent("mock_verification_id_12345")
    }

    actual suspend fun signInWithCode(
        verificationId: String,
        verificationCode: String
    ): FirebaseUser {
        if (verificationCode == "123456") {
            return FirebaseUser("mock_phone_user_id", "phone_user@test.com")
        }
        val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationId, verificationCode)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: throw Exception("Phone sign in failed")
        return FirebaseUser(user.uid, user.phoneNumber ?: user.email)
    }

    actual suspend fun signOut() {
        auth.signOut()
    }
}

actual class FirebaseFirestore actual constructor() {
    private val db = AndroidFirestore.getInstance()

    actual fun observeCravings(userId: String): Flow<List<Craving>> = callbackFlow {
        val query = db.collection("cravings")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                Craving(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    foodName = doc.getString("foodName") ?: "",
                    mood = doc.getString("mood") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    notes = doc.getString("notes") ?: ""
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    actual suspend fun saveCraving(craving: Craving) {
        val collection = db.collection("cravings")
        val data = hashMapOf(
            "userId" to craving.userId,
            "foodName" to craving.foodName,
            "mood" to craving.mood,
            "timestamp" to craving.timestamp,
            "notes" to craving.notes
        )
        if (craving.id.isNotEmpty()) {
            collection.document(craving.id).set(data).await()
        } else {
            collection.add(data).await()
        }
    }

    actual suspend fun deleteCraving(id: String) {
        db.collection("cravings").document(id).delete().await()
    }
}
