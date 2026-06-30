package com.pgstorm148.heyum.firebase

import com.pgstorm148.heyum.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface IosAuthDelegate {
    fun watchAuthState(onUserChanged: (uid: String?, email: String?) -> Unit)
    fun verifyPhoneNumber(phoneNumber: String, onCodeSent: (String) -> Unit, onFailed: (String) -> Unit)
    fun signInWithCode(verificationId: String, verificationCode: String, completion: (uid: String?, email: String?, error: String?) -> Unit)
    fun signIn(email: String, password: String, completion: (uid: String?, email: String?, error: String?) -> Unit)
    fun signUp(email: String, password: String, completion: (uid: String?, email: String?, error: String?) -> Unit)
    fun signOut(completion: (error: String?) -> Unit)
}

interface IosFirestoreDelegate {
    fun observeCravings(userId: String, onUpdate: (List<Craving>?, String?) -> Unit): () -> Unit
    fun saveCraving(id: String, userId: String, foodName: String, mood: String, timestamp: Long, notes: String, completion: (error: String?) -> Unit)
    fun deleteCraving(id: String, completion: (error: String?) -> Unit)
}

interface IosDishRepositoryDelegate {
    fun getDishesByContext(cuisine: Cuisine?, timeOfDay: TimeOfDay, weather: String, onUpdate: (List<Dish>?, String?) -> Unit): () -> Unit
    fun saveDish(dish: Dish, completion: (String?) -> Unit)
}

interface IosUserRepositoryDelegate {
    fun getUserProfile(userId: String, onUpdate: (UserProfile?, String?) -> Unit): () -> Unit
    fun saveUserProfile(profile: UserProfile, completion: (String?) -> Unit)
    fun updateMealHistory(userId: String, entry: MealHistoryEntry, completion: (String?) -> Unit)
    fun updateTasteVector(userId: String, tasteVector: Map<String, Float>, completion: (String?) -> Unit)
}

object IosFirebaseBridge {
    var authDelegate: IosAuthDelegate? = null
    var firestoreDelegate: IosFirestoreDelegate? = null
    var dishRepositoryDelegate: IosDishRepositoryDelegate? = null
    var userRepositoryDelegate: IosUserRepositoryDelegate? = null
    var messagingDelegate: IosMessagingDelegate? = null
}

actual class FirebaseAuth actual constructor() {
    private val delegate: IosAuthDelegate
        get() = IosFirebaseBridge.authDelegate ?: throw IllegalStateException("IosAuthDelegate is not initialized. Register it from Swift on app startup.")

    private var cachedUser: FirebaseUser? = null

    init {
        try {
            delegate.watchAuthState { uid, email ->
                cachedUser = if (uid != null) FirebaseUser(uid, email) else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual val currentUser: Flow<FirebaseUser?> = callbackFlow {
        delegate.watchAuthState { uid, email ->
            val user = if (uid != null) FirebaseUser(uid, email) else null
            cachedUser = user
            trySend(user)
        }
        awaitClose { /* empty cleanup */ }
    }

    actual val currentCachedUser: FirebaseUser?
        get() = cachedUser

    actual fun verifyPhoneNumber(
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onFailed: (String) -> Unit
    ) {
        delegate.verifyPhoneNumber(phoneNumber, onCodeSent, onFailed)
    }

    actual suspend fun signInWithCode(
        verificationId: String,
        verificationCode: String
    ): FirebaseUser = suspendCancellableCoroutine { continuation ->
        delegate.signInWithCode(verificationId, verificationCode) { uid, userEmail, error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error))
            } else if (uid != null) {
                continuation.resume(FirebaseUser(uid, userEmail))
            } else {
                continuation.resumeWithException(Exception("Unknown phone sign in error"))
            }
        }
    }

    actual suspend fun signOut() = suspendCancellableCoroutine<Unit> { continuation ->
        delegate.signOut { error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error))
            } else {
                continuation.resume(Unit)
            }
        }
    }

    // Legacy helpers if needed by WelcomeScreen
    actual suspend fun signIn(email: String, password: String): FirebaseUser = suspendCancellableCoroutine { continuation ->
        delegate.signIn(email, password) { uid, userEmail, error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error))
            } else if (uid != null) {
                continuation.resume(FirebaseUser(uid, userEmail))
            } else {
                continuation.resumeWithException(Exception("Unknown sign in error"))
            }
        }
    }

    actual suspend fun signUp(email: String, password: String): FirebaseUser = suspendCancellableCoroutine { continuation ->
        delegate.signUp(email, password) { uid, userEmail, error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error))
            } else if (uid != null) {
                continuation.resume(FirebaseUser(uid, userEmail))
            } else {
                continuation.resumeWithException(Exception("Unknown sign up error"))
            }
        }
    }
}

actual class FirebaseFirestore actual constructor() {
    private val delegate: IosFirestoreDelegate
        get() = IosFirebaseBridge.firestoreDelegate ?: throw IllegalStateException("IosFirestoreDelegate is not initialized. Register it from Swift on app startup.")

    actual fun observeCravings(userId: String): Flow<List<Craving>> = callbackFlow {
        val unsubscribe = delegate.observeCravings(userId) { list, error ->
            if (error != null) {
                close(Exception(error))
            } else if (list != null) {
                trySend(list)
            }
        }
        awaitClose { unsubscribe() }
    }

    actual suspend fun saveCraving(craving: Craving) = suspendCancellableCoroutine<Unit> { continuation ->
        delegate.saveCraving(
            id = craving.id,
            userId = craving.userId,
            foodName = craving.foodName,
            mood = craving.mood,
            timestamp = craving.timestamp,
            notes = craving.notes
        ) { error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error))
            } else {
                continuation.resume(Unit)
            }
        }
    }

    actual suspend fun deleteCraving(id: String) = suspendCancellableCoroutine<Unit> { continuation ->
        delegate.deleteCraving(id) { error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error))
            } else {
                continuation.resume(Unit)
            }
        }
    }
}

class IosDishRepository : DishRepository {
    private val delegate: IosDishRepositoryDelegate
        get() = IosFirebaseBridge.dishRepositoryDelegate ?: throw IllegalStateException("IosDishRepositoryDelegate is not initialized")

    override fun getDishesByContext(cuisine: Cuisine?, timeOfDay: TimeOfDay, weather: String): Flow<List<Dish>> = callbackFlow {
        val unsubscribe = delegate.getDishesByContext(cuisine, timeOfDay, weather) { list, error ->
            if (error != null) {
                close(Exception(error))
            } else if (list != null) {
                trySend(list)
            }
        }
        awaitClose { unsubscribe() }
    }

    override suspend fun saveDish(dish: Dish) = suspendCancellableCoroutine<Unit> { continuation ->
        delegate.saveDish(dish) { error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error))
            } else {
                continuation.resume(Unit)
            }
        }
    }
}

class IosUserRepository : UserRepository {
    private val delegate: IosUserRepositoryDelegate
        get() = IosFirebaseBridge.userRepositoryDelegate ?: throw IllegalStateException("IosUserRepositoryDelegate is not initialized")

    override fun getUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val unsubscribe = delegate.getUserProfile(userId) { profile, error ->
            if (error != null) {
                close(Exception(error))
            } else {
                trySend(profile)
            }
        }
        awaitClose { unsubscribe() }
    }

    override suspend fun saveUserProfile(profile: UserProfile) = suspendCancellableCoroutine<Unit> { continuation ->
        delegate.saveUserProfile(profile) { error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error))
            } else {
                continuation.resume(Unit)
            }
        }
    }

    override suspend fun updateMealHistory(userId: String, entry: MealHistoryEntry) = suspendCancellableCoroutine<Unit> { continuation ->
        delegate.updateMealHistory(userId, entry) { error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error))
            } else {
                continuation.resume(Unit)
            }
        }
    }

    override suspend fun updateTasteVector(userId: String, tasteVector: Map<String, Float>) = suspendCancellableCoroutine<Unit> { continuation ->
        delegate.updateTasteVector(userId, tasteVector) { error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error))
            } else {
                continuation.resume(Unit)
            }
        }
    }
}

class IosGroupSessionRepository : GroupSessionRepository {
    override fun observeSession(code: String) = kotlinx.coroutines.flow.emptyFlow<GroupSession?>()
    override suspend fun createSession(session: GroupSession) {}
    override suspend fun joinSession(code: String, member: GroupMember) {}
    override suspend fun makeDecision(code: String, dishId: String) {}
    override suspend fun cycleSuggestions(code: String, dishIds: List<String>) {}
}
