package com.pgstorm148.heyum.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore as AndroidFirestore
import com.google.firebase.firestore.Query
import com.pgstorm148.heyum.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AndroidDishRepository : DishRepository {
    private val db = AndroidFirestore.getInstance()

    override fun getDishesByContext(cuisine: Cuisine?, timeOfDay: TimeOfDay, weather: String): Flow<List<Dish>> = callbackFlow {
        // Query dishes from Firestore
        var query: Query = db.collection("dishes")
        
        // Match on cuisine if specified
        if (cuisine != null) {
            query = query.whereEqualTo("cuisine", cuisine.name)
        }
        
        // Real-time snapshot listener
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toDish()
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun saveDish(dish: Dish) {
        db.collection("dishes").document(dish.id).set(dish.toMap()).await()
    }
}

class AndroidUserRepository : UserRepository {
    private val db = AndroidFirestore.getInstance()

    override fun getUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val ref = db.collection("users").document(userId)
        val registration = ref.addSnapshotListener { doc, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (doc != null && doc.exists()) {
                trySend(doc.toUserProfile())
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        db.collection("users").document(profile.id).set(profile.toMap()).await()
    }

    override suspend fun updateMealHistory(userId: String, entry: MealHistoryEntry) {
        val ref = db.collection("users").document(userId)
        ref.update("mealHistory", FieldValue.arrayUnion(entry.toMap())).await()
    }

    override suspend fun updateTasteVector(userId: String, tasteVector: Map<String, Float>) {
        val ref = db.collection("users").document(userId)
        ref.update("tasteVector", tasteVector).await()
    }
}

// --- Serializers and Deserializers for Android ---

private fun DocumentSnapshot.toDish(): Dish {
    val cuisineStr = getString("cuisine") ?: "COMFORT"
    val difficultyStr = getString("difficulty") ?: "MEDIUM"
    return Dish(
        id = id,
        name = getString("name") ?: "",
        cuisine = try { Cuisine.valueOf(cuisineStr) } catch(e: Exception) { Cuisine.COMFORT },
        description = getString("description") ?: "",
        imageUrl = getString("imageUrl") ?: "",
        spiceLevel = getLong("spiceLevel")?.toInt() ?: 3,
        prepTimeMinutes = getLong("prepTimeMinutes")?.toInt() ?: 30,
        difficulty = try { Difficulty.valueOf(difficultyStr) } catch(e: Exception) { Difficulty.MEDIUM },
        ingredients = (get("ingredients") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
        instructions = (get("instructions") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
        dietaryTags = (get("dietaryTags") as? List<*>)?.mapNotNull { 
            try { DietaryRestriction.valueOf(it.toString()) } catch(e: Exception) { null } 
        } ?: emptyList(),
        occasions = (get("occasions") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
        orderingAvailable = getBoolean("orderingAvailable") ?: false,
        orderingUrl = getString("orderingUrl")
    )
}

private fun DocumentSnapshot.toUserProfile(): UserProfile {
    val mealHistoryList = (get("mealHistory") as? List<*>)?.mapNotNull { map ->
        val m = map as? Map<*, *> ?: return@mapNotNull null
        val cuisineStr = m["cuisine"]?.toString() ?: "COMFORT"
        val contextMap = m["context"] as? Map<*, *> ?: return@mapNotNull null
        val timeOfDayStr = contextMap["timeOfDay"]?.toString() ?: "MORNING"
        val moodStr = contextMap["mood"]?.toString()
        
        MealHistoryEntry(
            id = m["id"]?.toString() ?: "",
            dishName = m["dishName"]?.toString() ?: "",
            cuisine = try { Cuisine.valueOf(cuisineStr) } catch(e: Exception) { Cuisine.COMFORT },
            accepted = m["accepted"] as? Boolean ?: true,
            timestamp = (m["timestamp"] as? Number)?.toLong() ?: 0L,
            context = MealContext(
                timeOfDay = try { TimeOfDay.valueOf(timeOfDayStr) } catch(e: Exception) { TimeOfDay.MORNING },
                weather = contextMap["weather"]?.toString() ?: "",
                mood = moodStr?.let { try { Mood.valueOf(it) } catch(e: Exception) { null } },
                groupSize = (contextMap["groupSize"] as? Number)?.toInt() ?: 1,
                availableIngredients = (contextMap["availableIngredients"] as? List<*>)?.mapNotNull { it?.toString() }
            )
        )
    } ?: emptyList()

    val rawTasteVector = get("tasteVector") as? Map<*, *>
    val tasteVectorMap = mutableMapOf<String, Float>()
    rawTasteVector?.forEach { (key, value) ->
        if (key != null && value is Number) {
            tasteVectorMap[key.toString()] = value.toFloat()
        }
    }

    return UserProfile(
        id = id,
        name = getString("name") ?: "",
        dietaryRestrictions = (get("dietaryRestrictions") as? List<*>)?.mapNotNull { 
            try { DietaryRestriction.valueOf(it.toString()) } catch(e: Exception) { null }
        } ?: emptyList(),
        spiceTolerance = getLong("spiceTolerance")?.toInt() ?: 3,
        favoriteCuisines = (get("favoriteCuisines") as? List<*>)?.mapNotNull { 
            try { Cuisine.valueOf(it.toString()) } catch(e: Exception) { null }
        } ?: emptyList(),
        dislikedIngredients = (get("dislikedIngredients") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
        mealHistory = mealHistoryList,
        tasteVector = tasteVectorMap,
        onboardingCompleted = getBoolean("onboardingCompleted") ?: false,
        notificationFrequency = try { NotificationFrequency.valueOf(getString("notificationFrequency") ?: "STANDARD") } catch(e: Exception) { NotificationFrequency.STANDARD },
        quietHoursStart = getString("quietHoursStart") ?: "23:00",
        quietHoursEnd = getString("quietHoursEnd") ?: "08:00",
        quietHoursEnabled = getBoolean("quietHoursEnabled") ?: false,
        mealRemindersEnabled = getBoolean("mealRemindersEnabled") ?: true,
        reorderSuggestionsEnabled = getBoolean("reorderSuggestionsEnabled") ?: true,
        wasteAlertsEnabled = getBoolean("wasteAlertsEnabled") ?: true,
        groupInvitesEnabled = getBoolean("groupInvitesEnabled") ?: true
    )
}

private fun Dish.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["id"] = id
    map["name"] = name
    map["cuisine"] = cuisine.name
    map["description"] = description
    map["imageUrl"] = imageUrl
    map["spiceLevel"] = spiceLevel
    map["prepTimeMinutes"] = prepTimeMinutes
    map["difficulty"] = difficulty.name
    map["ingredients"] = ingredients
    map["instructions"] = instructions
    map["dietaryTags"] = dietaryTags.map { it.name }
    map["occasions"] = occasions
    map["orderingAvailable"] = orderingAvailable
    orderingUrl?.let { map["orderingUrl"] = it }
    return map
}

private fun UserProfile.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["name"] = name
    map["dietaryRestrictions"] = dietaryRestrictions.map { it.name }
    map["spiceTolerance"] = spiceTolerance
    map["favoriteCuisines"] = favoriteCuisines.map { it.name }
    map["dislikedIngredients"] = dislikedIngredients
    map["mealHistory"] = mealHistory.map { it.toMap() }
    map["tasteVector"] = tasteVector
    map["onboardingCompleted"] = onboardingCompleted
    map["notificationFrequency"] = notificationFrequency.name
    map["quietHoursStart"] = quietHoursStart
    map["quietHoursEnd"] = quietHoursEnd
    map["quietHoursEnabled"] = quietHoursEnabled
    map["mealRemindersEnabled"] = mealRemindersEnabled
    map["reorderSuggestionsEnabled"] = reorderSuggestionsEnabled
    map["wasteAlertsEnabled"] = wasteAlertsEnabled
    map["groupInvitesEnabled"] = groupInvitesEnabled
    return map
}

private fun MealHistoryEntry.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["id"] = id
    map["dishName"] = dishName
    map["cuisine"] = cuisine.name
    map["accepted"] = accepted
    map["timestamp"] = timestamp
    map["context"] = context.toMap()
    return map
}

private fun MealContext.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["timeOfDay"] = timeOfDay.name
    map["weather"] = weather
    map["groupSize"] = groupSize
    mood?.let { map["mood"] = it.name }
    availableIngredients?.let { map["availableIngredients"] = it }
    return map
}

class AndroidGroupSessionRepository : GroupSessionRepository {
    private val db = AndroidFirestore.getInstance()

    override fun observeSession(code: String): Flow<GroupSession?> = callbackFlow {
        val ref = db.collection("sessions").whereEqualTo("code", code)
        val registration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val doc = snapshot?.documents?.firstOrNull()
            if (doc != null && doc.exists()) {
                trySend(doc.toGroupSession())
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun createSession(session: GroupSession) {
        db.collection("sessions").document(session.id).set(session.toMap()).await()
    }

    override suspend fun joinSession(code: String, member: GroupMember) {
        val snapshot = db.collection("sessions").whereEqualTo("code", code).get().await()
        val doc = snapshot.documents.firstOrNull()
        if (doc != null && doc.exists()) {
            doc.reference.update("members", FieldValue.arrayUnion(member.toMap())).await()
        }
    }

    override suspend fun makeDecision(code: String, dishId: String) {
        val snapshot = db.collection("sessions").whereEqualTo("code", code).get().await()
        val doc = snapshot.documents.firstOrNull()
        if (doc != null && doc.exists()) {
            doc.reference.update("finalDecision", dishId).await()
        }
    }

    override suspend fun cycleSuggestions(code: String, dishIds: List<String>) {
        val snapshot = db.collection("sessions").whereEqualTo("code", code).get().await()
        val doc = snapshot.documents.firstOrNull()
        if (doc != null && doc.exists()) {
            doc.reference.update("currentSuggestions", dishIds).await()
        }
    }
}

private fun DocumentSnapshot.toGroupSession(): GroupSession {
    val membersList = (get("members") as? List<*>)?.mapNotNull { map ->
        val m = map as? Map<*, *> ?: return@mapNotNull null
        val diets = (m["dietaryRestrictions"] as? List<*>)?.mapNotNull {
            try { DietaryRestriction.valueOf(it.toString()) } catch (e: Exception) { null }
        } ?: emptyList()
        val cuisines = (m["favoriteCuisines"] as? List<*>)?.mapNotNull {
            try { Cuisine.valueOf(it.toString()) } catch (e: Exception) { null }
        } ?: emptyList()

        GroupMember(
            userId = m["userId"]?.toString() ?: "",
            name = m["name"]?.toString() ?: "",
            initials = m["initials"]?.toString() ?: "",
            colorHex = m["colorHex"]?.toString() ?: "",
            dietaryRestrictions = diets,
            favoriteCuisines = cuisines,
            spiceTolerance = (m["spiceTolerance"] as? Number)?.toInt() ?: 3
        )
    } ?: emptyList()

    return GroupSession(
        id = id,
        code = getString("code") ?: "",
        hostId = getString("hostId") ?: "",
        members = membersList,
        currentSuggestions = (get("currentSuggestions") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
        finalDecision = getString("finalDecision"),
        expiresAt = getLong("expiresAt") ?: 0L
    )
}

private fun GroupSession.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["code"] = code
    map["hostId"] = hostId
    map["members"] = members.map { it.toMap() }
    map["currentSuggestions"] = currentSuggestions
    finalDecision?.let { map["finalDecision"] = it }
    map["expiresAt"] = expiresAt
    return map
}

private fun GroupMember.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["userId"] = userId
    map["name"] = name
    map["initials"] = initials
    map["colorHex"] = colorHex
    map["dietaryRestrictions"] = dietaryRestrictions.map { it.name }
    map["favoriteCuisines"] = favoriteCuisines.map { it.name }
    map["spiceTolerance"] = spiceTolerance
    return map
}

