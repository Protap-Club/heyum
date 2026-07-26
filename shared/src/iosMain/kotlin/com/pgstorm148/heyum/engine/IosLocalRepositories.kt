package com.pgstorm148.heyum.engine

import com.pgstorm148.heyum.model.MealLogEntry
import com.pgstorm148.heyum.model.RejectedDish
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.Foundation.dateWithTimeIntervalSince1970

// Mirrors AndroidMealLogRepository (Room, "date(timestamp/1000, 'unixepoch', 'localtime')")
// exactly: dates are compared in local time. NSUserDefaults (not a real DB) is enough for this
// data's size, and keeps parity with how KitchenStorage/WidgetCache already persist on iOS.
private const val MEAL_LOGS_KEY = "heyum.mealLogs"

// NSDateFormatter defaults to the system's current time zone, matching Android's
// 'localtime' modifier in AndroidMealLogRepository's date queries.
private val localDateFormatter = NSDateFormatter().apply {
    dateFormat = "yyyy-MM-dd"
}

private fun localDateString(timestampMillis: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestampMillis / 1000.0)
    return localDateFormatter.stringFromDate(date)
}

private fun todayDateString(): String = localDateFormatter.stringFromDate(NSDate())

class IosMealLogRepository : MealLogRepository {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val logsFlow = MutableStateFlow(loadFromDisk())

    private fun loadFromDisk(): List<MealLogEntry> {
        val raw = defaults.arrayForKey(MEAL_LOGS_KEY) as? List<*> ?: return emptyList()
        return raw.mapNotNull { item ->
            val dict = item as? Map<*, *> ?: return@mapNotNull null
            MealLogEntry(
                id = dict["id"] as? String ?: return@mapNotNull null,
                dishId = dict["dishId"] as? String ?: "",
                dishName = dict["dishName"] as? String ?: "",
                cuisine = dict["cuisine"] as? String ?: "",
                mealType = dict["mealType"] as? String ?: "",
                timestamp = (dict["timestamp"] as? Number)?.toLong() ?: 0L,
                accepted = dict["accepted"] as? Boolean ?: true,
                rating = dict["rating"] as? String
            )
        }.sortedByDescending { it.timestamp }
    }

    private fun persist(logs: List<MealLogEntry>) {
        val sorted = logs.sortedByDescending { it.timestamp }
        val array = sorted.map { entry ->
            val dict = mutableMapOf<String, Any>(
                "id" to entry.id,
                "dishId" to entry.dishId,
                "dishName" to entry.dishName,
                "cuisine" to entry.cuisine,
                "mealType" to entry.mealType,
                "timestamp" to entry.timestamp,
                "accepted" to entry.accepted
            )
            entry.rating?.let { dict["rating"] = it }
            dict
        }
        defaults.setObject(array, forKey = MEAL_LOGS_KEY)
        logsFlow.value = sorted
    }

    override fun getAllLogs(): Flow<List<MealLogEntry>> = logsFlow

    override fun getTodayLogs(): Flow<List<MealLogEntry>> {
        val today = todayDateString()
        return logsFlow.map { logs -> logs.filter { localDateString(it.timestamp) == today } }
    }

    override fun getLogsByDate(dateString: String): Flow<List<MealLogEntry>> {
        return logsFlow.map { logs -> logs.filter { localDateString(it.timestamp) == dateString } }
    }

    override suspend fun insert(entry: MealLogEntry) {
        val withId = if (entry.id.isBlank()) entry.copy(id = generateId()) else entry
        persist(logsFlow.value.filterNot { it.id == withId.id } + withId)
    }

    override suspend fun delete(entry: MealLogEntry) {
        persist(logsFlow.value.filterNot { it.id == entry.id })
    }

    override suspend fun updateRating(id: String, rating: String) {
        persist(logsFlow.value.map { if (it.id == id) it.copy(rating = rating) else it })
    }
}

class IosPreferenceRepository : PreferenceRepository {
    private val defaults = NSUserDefaults.standardUserDefaults

    private val rejectedDishesKey = "heyum.rejectedDishes"
    private val currentStreakKey = "heyum.currentStreak"
    private val totalMealsKey = "heyum.totalMealsLogged"
    private val uniqueCuisinesKey = "heyum.uniqueCuisinesTried"

    override suspend fun getRejectedDishes(): Map<String, RejectedDish> {
        val raw = defaults.arrayForKey(rejectedDishesKey) as? List<*> ?: return emptyMap()
        return raw.mapNotNull { item ->
            val dict = item as? Map<*, *> ?: return@mapNotNull null
            val dishId = dict["dishId"] as? String ?: return@mapNotNull null
            dishId to RejectedDish(
                dishId = dishId,
                rejectedAt = (dict["rejectedAt"] as? Number)?.toLong() ?: 0L,
                mealType = dict["mealType"] as? String ?: ""
            )
        }.toMap()
    }

    override suspend fun saveRejectedDishes(dishes: Map<String, RejectedDish>) {
        val array = dishes.values.map { rejected ->
            mapOf(
                "dishId" to rejected.dishId,
                "rejectedAt" to rejected.rejectedAt,
                "mealType" to rejected.mealType
            )
        }
        defaults.setObject(array, forKey = rejectedDishesKey)
    }

    override suspend fun getStreakInfo(): StreakInfo {
        val currentStreak = defaults.objectForKey(currentStreakKey) as? Number
        val totalMeals = defaults.objectForKey(totalMealsKey) as? Number
        val cuisines = defaults.arrayForKey(uniqueCuisinesKey) as? List<*>
        return StreakInfo(
            currentStreak = currentStreak?.toInt() ?: 0,
            totalMealsLogged = totalMeals?.toInt() ?: 0,
            uniqueCuisinesTried = cuisines?.mapNotNull { it as? String } ?: emptyList()
        )
    }

    override suspend fun saveStreakInfo(streakInfo: StreakInfo) {
        defaults.setObject(streakInfo.currentStreak, forKey = currentStreakKey)
        defaults.setObject(streakInfo.totalMealsLogged, forKey = totalMealsKey)
        defaults.setObject(streakInfo.uniqueCuisinesTried, forKey = uniqueCuisinesKey)
    }

    override suspend fun clearPreferences() {
        defaults.removeObjectForKey(rejectedDishesKey)
        defaults.removeObjectForKey(currentStreakKey)
        defaults.removeObjectForKey(totalMealsKey)
        defaults.removeObjectForKey(uniqueCuisinesKey)
    }
}

private fun generateId(): String = NSUUID().UUIDString
