package com.pgstorm148.heyum.firebase

import com.pgstorm148.heyum.model.*
import kotlinx.coroutines.flow.Flow

expect class FirebaseAuth() {
    val currentUser: Flow<FirebaseUser?>
    val currentCachedUser: FirebaseUser?
    suspend fun signIn(email: String, password: String): FirebaseUser
    suspend fun signUp(email: String, password: String): FirebaseUser
    suspend fun signOut()
    
    // Phone Authentication API
    fun verifyPhoneNumber(
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onFailed: (String) -> Unit
    )
    suspend fun signInWithCode(verificationId: String, verificationCode: String): FirebaseUser
}

expect class FirebaseFirestore() {
    fun observeCravings(userId: String): Flow<List<Craving>>
    suspend fun saveCraving(craving: Craving)
    suspend fun deleteCraving(id: String)
}

interface DishRepository {
    fun getDishesByContext(cuisine: Cuisine?, timeOfDay: TimeOfDay, weather: String): Flow<List<Dish>>
    suspend fun saveDish(dish: Dish)
}

interface UserRepository {
    fun getUserProfile(userId: String): Flow<UserProfile?>
    suspend fun saveUserProfile(profile: UserProfile)
    suspend fun updateMealHistory(userId: String, entry: MealHistoryEntry)
    suspend fun updateTasteVector(userId: String, tasteVector: Map<String, Float>)
}

interface AmbienceRepository {
    /** Emits the current server-driven ambience config; degrades to [AmbienceConfig.Default] on error. */
    fun observeAmbience(): Flow<AmbienceConfig>
}

interface GroupSessionRepository {
    fun observeSession(code: String): Flow<GroupSession?>
    suspend fun createSession(session: GroupSession)
    suspend fun joinSession(code: String, member: GroupMember)
    suspend fun makeDecision(code: String, dishId: String)
    suspend fun updateSelection(code: String, userId: String, cuisine: String, dish: String)
    suspend fun startCollaboration(code: String)
    suspend fun endSession(code: String)
    suspend fun leaveSession(code: String, userId: String)
}
