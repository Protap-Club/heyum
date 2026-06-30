package com.pgstorm148.heyum.engine

import com.pgstorm148.heyum.model.MealLogEntry
import com.pgstorm148.heyum.model.RejectedDish
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class IosMealLogRepository : MealLogRepository {
    override fun getAllLogs(): Flow<List<MealLogEntry>> = emptyFlow()
    override fun getTodayLogs(): Flow<List<MealLogEntry>> = emptyFlow()
    override fun getLogsByDate(dateString: String): Flow<List<MealLogEntry>> = emptyFlow()
    override suspend fun insert(entry: MealLogEntry) {}
    override suspend fun delete(entry: MealLogEntry) {}
    override suspend fun updateRating(id: String, rating: String) {}
}

class IosPreferenceRepository : PreferenceRepository {
    private var rejectedDishes: Map<String, RejectedDish> = emptyMap()
    private var streakInfo: StreakInfo = StreakInfo()

    override suspend fun getRejectedDishes(): Map<String, RejectedDish> = rejectedDishes
    
    override suspend fun saveRejectedDishes(dishes: Map<String, RejectedDish>) { 
        rejectedDishes = dishes 
    }
    
    override suspend fun getStreakInfo(): StreakInfo = streakInfo
    
    override suspend fun saveStreakInfo(streakInfo: StreakInfo) { 
        this.streakInfo = streakInfo 
    }
    
    override suspend fun clearPreferences() {
        rejectedDishes = emptyMap()
        streakInfo = StreakInfo()
    }
}
