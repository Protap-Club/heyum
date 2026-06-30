package com.pgstorm148.heyum.engine

import com.pgstorm148.heyum.model.MealLogEntry
import com.pgstorm148.heyum.model.RejectedDish
import kotlinx.coroutines.flow.Flow

data class StreakInfo(
    val currentStreak: Int = 0,
    val totalMealsLogged: Int = 0,
    val uniqueCuisinesTried: List<String> = emptyList()
)

interface MealLogRepository {
    fun getAllLogs(): Flow<List<MealLogEntry>>
    fun getTodayLogs(): Flow<List<MealLogEntry>>
    fun getLogsByDate(dateString: String): Flow<List<MealLogEntry>>
    suspend fun insert(entry: MealLogEntry)
    suspend fun delete(entry: MealLogEntry)
    suspend fun updateRating(id: String, rating: String)
}

interface PreferenceRepository {
    suspend fun getRejectedDishes(): Map<String, RejectedDish>
    suspend fun saveRejectedDishes(dishes: Map<String, RejectedDish>)
    suspend fun getStreakInfo(): StreakInfo
    suspend fun saveStreakInfo(streakInfo: StreakInfo)
    suspend fun clearPreferences()
}
