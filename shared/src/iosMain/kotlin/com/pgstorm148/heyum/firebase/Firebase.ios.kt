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

interface IosAmbienceRepositoryDelegate {
    fun observeAmbience(onUpdate: (Map<String, Any?>?, String?) -> Unit): () -> Unit
}

interface IosPromoRepositoryDelegate {
    fun observePromo(onUpdate: (Map<String, Any?>?, String?) -> Unit): () -> Unit
}

interface IosGroupSessionRepositoryDelegate {
    fun observeSession(code: String, onUpdate: (GroupSession?, String?) -> Unit): () -> Unit
    fun createSession(session: GroupSession, completion: (String?) -> Unit)
    fun joinSession(code: String, member: GroupMember, completion: (String?) -> Unit)
    fun makeDecision(code: String, dishId: String, completion: (String?) -> Unit)
    fun updateSelection(code: String, userId: String, cuisine: String, dish: String, completion: (String?) -> Unit)
    fun startCollaboration(code: String, completion: (String?) -> Unit)
    fun endSession(code: String, completion: (String?) -> Unit)
    fun leaveSession(code: String, userId: String, completion: (String?) -> Unit)
}

object IosFirebaseBridge {
    var authDelegate: IosAuthDelegate? = null
    var firestoreDelegate: IosFirestoreDelegate? = null
    var dishRepositoryDelegate: IosDishRepositoryDelegate? = null
    var userRepositoryDelegate: IosUserRepositoryDelegate? = null
    var ambienceRepositoryDelegate: IosAmbienceRepositoryDelegate? = null
    var promoRepositoryDelegate: IosPromoRepositoryDelegate? = null
    var groupSessionRepositoryDelegate: IosGroupSessionRepositoryDelegate? = null
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
    // Until Swift registers a delegate, this repo degrades gracefully (empty data) rather than
    // crashing, so the app still launches on iOS.
    private val delegate: IosDishRepositoryDelegate?
        get() = IosFirebaseBridge.dishRepositoryDelegate

    override fun getDishesByContext(cuisine: Cuisine?, timeOfDay: TimeOfDay, weather: String): Flow<List<Dish>> = callbackFlow {
        val d = delegate
        if (d == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val unsubscribe = d.getDishesByContext(cuisine, timeOfDay, weather) { list, error ->
            // Mirrors AndroidDishRepository: degrade to empty on error rather than closing the
            // flow, since this is a long-lived listener (e.g. GroupSessionScreen keeps it open
            // for the whole session) that should recover from a transient error, not die forever.
            if (error != null) {
                trySend(emptyList())
            } else if (list != null) {
                trySend(list)
            }
        }
        awaitClose { unsubscribe() }
    }

    override suspend fun saveDish(dish: Dish) {
        val d = delegate ?: return
        suspendCancellableCoroutine<Unit> { continuation ->
            d.saveDish(dish) { error ->
                if (error != null) continuation.resumeWithException(Exception(error))
                else continuation.resume(Unit)
            }
        }
    }
}

class IosUserRepository : UserRepository {
    // Degrades gracefully (null profile / no-op writes) when Swift hasn't registered a delegate,
    // so the app launches on iOS instead of crashing.
    private val delegate: IosUserRepositoryDelegate?
        get() = IosFirebaseBridge.userRepositoryDelegate

    override fun getUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val d = delegate
        if (d == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val unsubscribe = d.getUserProfile(userId) { profile, error ->
            if (error != null) {
                close(Exception(error))
            } else {
                trySend(profile)
            }
        }
        awaitClose { unsubscribe() }
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        val d = delegate ?: return
        suspendCancellableCoroutine<Unit> { continuation ->
            d.saveUserProfile(profile) { error ->
                if (error != null) continuation.resumeWithException(Exception(error))
                else continuation.resume(Unit)
            }
        }
    }

    override suspend fun updateMealHistory(userId: String, entry: MealHistoryEntry) {
        val d = delegate ?: return
        suspendCancellableCoroutine<Unit> { continuation ->
            d.updateMealHistory(userId, entry) { error ->
                if (error != null) continuation.resumeWithException(Exception(error))
                else continuation.resume(Unit)
            }
        }
    }

    override suspend fun updateTasteVector(userId: String, tasteVector: Map<String, Float>) {
        val d = delegate ?: return
        suspendCancellableCoroutine<Unit> { continuation ->
            d.updateTasteVector(userId, tasteVector) { error ->
                if (error != null) continuation.resumeWithException(Exception(error))
                else continuation.resume(Unit)
            }
        }
    }
}

class IosAmbienceRepository : AmbienceRepository {
    // Degrades to AmbienceConfig.Default (ambience off) when Swift hasn't registered a delegate,
    // so the decision screen renders normally instead of crashing.
    private val delegate: IosAmbienceRepositoryDelegate?
        get() = IosFirebaseBridge.ambienceRepositoryDelegate

    override fun observeAmbience(): Flow<AmbienceConfig> = callbackFlow {
        val d = delegate
        if (d == null) {
            trySend(AmbienceConfig.Default)
            awaitClose { }
            return@callbackFlow
        }
        val unsubscribe = d.observeAmbience { data, error ->
            if (error != null) {
                trySend(AmbienceConfig.Default)
            } else {
                trySend(AmbienceConfig.fromMap(data))
            }
        }
        awaitClose { unsubscribe() }
    }
}

class IosPromoRepository : PromoRepository {
    // Degrades to PromoConfig.Default (popup hidden) when Swift hasn't registered a delegate.
    private val delegate: IosPromoRepositoryDelegate?
        get() = IosFirebaseBridge.promoRepositoryDelegate

    override fun observePromo(): Flow<PromoConfig> = callbackFlow {
        val d = delegate
        if (d == null) {
            trySend(PromoConfig.Default)
            awaitClose { }
            return@callbackFlow
        }
        val unsubscribe = d.observePromo { data, error ->
            if (error != null) {
                trySend(PromoConfig.Default)
            } else {
                trySend(PromoConfig.fromMap(data))
            }
        }
        awaitClose { unsubscribe() }
    }
}

class IosGroupSessionRepository : GroupSessionRepository {
    // Degrades gracefully (null session / no-op writes) when Swift hasn't registered a delegate,
    // so the group flow never crashes — it just can't do anything until Swift is wired up.
    private val delegate: IosGroupSessionRepositoryDelegate?
        get() = IosFirebaseBridge.groupSessionRepositoryDelegate

    override fun observeSession(code: String): Flow<GroupSession?> = callbackFlow {
        val d = delegate
        if (d == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val unsubscribe = d.observeSession(code) { session, error ->
            if (error != null) trySend(null) else trySend(session)
        }
        awaitClose { unsubscribe() }
    }

    override suspend fun createSession(session: GroupSession) {
        val d = delegate ?: return
        suspendCancellableCoroutine<Unit> { continuation ->
            d.createSession(session) { error ->
                if (error != null) continuation.resumeWithException(Exception(error))
                else continuation.resume(Unit)
            }
        }
    }

    override suspend fun joinSession(code: String, member: GroupMember) {
        val d = delegate ?: return
        suspendCancellableCoroutine<Unit> { continuation ->
            d.joinSession(code, member) { error ->
                if (error != null) continuation.resumeWithException(Exception(error))
                else continuation.resume(Unit)
            }
        }
    }

    override suspend fun makeDecision(code: String, dishId: String) {
        val d = delegate ?: return
        suspendCancellableCoroutine<Unit> { continuation ->
            d.makeDecision(code, dishId) { error ->
                if (error != null) continuation.resumeWithException(Exception(error))
                else continuation.resume(Unit)
            }
        }
    }

    override suspend fun updateSelection(code: String, userId: String, cuisine: String, dish: String) {
        val d = delegate ?: return
        suspendCancellableCoroutine<Unit> { continuation ->
            d.updateSelection(code, userId, cuisine, dish) { error ->
                if (error != null) continuation.resumeWithException(Exception(error))
                else continuation.resume(Unit)
            }
        }
    }

    override suspend fun startCollaboration(code: String) {
        val d = delegate ?: return
        suspendCancellableCoroutine<Unit> { continuation ->
            d.startCollaboration(code) { error ->
                if (error != null) continuation.resumeWithException(Exception(error))
                else continuation.resume(Unit)
            }
        }
    }

    override suspend fun endSession(code: String) {
        val d = delegate ?: return
        suspendCancellableCoroutine<Unit> { continuation ->
            d.endSession(code) { error ->
                if (error != null) continuation.resumeWithException(Exception(error))
                else continuation.resume(Unit)
            }
        }
    }

    override suspend fun leaveSession(code: String, userId: String) {
        val d = delegate ?: return
        suspendCancellableCoroutine<Unit> { continuation ->
            d.leaveSession(code, userId) { error ->
                if (error != null) continuation.resumeWithException(Exception(error))
                else continuation.resume(Unit)
            }
        }
    }
}
