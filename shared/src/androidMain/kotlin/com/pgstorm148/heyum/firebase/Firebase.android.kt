package com.pgstorm148.heyum.firebase

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth as AndroidAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore as AndroidFirestore
import com.google.firebase.firestore.Query
import com.pgstorm148.heyum.model.FirebaseUser
import com.pgstorm148.heyum.model.Craving
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

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
        val activity = AndroidActivityHolder.getActivity()
        if (activity == null) {
            onFailed("Unable to start phone verification. Please reopen the app and try again.")
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Firebase can auto-resolve some numbers; fall back to sign-in immediately.
                onFailed("Phone verification completed automatically. Please try again in the app.")
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                onFailed(exception.message ?: "Phone verification failed")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                onCodeSent(verificationId)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    actual suspend fun signInWithCode(
        verificationId: String,
        verificationCode: String
    ): FirebaseUser {
        val credential = PhoneAuthProvider.getCredential(verificationId, verificationCode)
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
