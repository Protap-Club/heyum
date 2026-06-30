package com.pgstorm148.heyum.db

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pgstorm148.heyum.engine.PreferenceRepository
import com.pgstorm148.heyum.engine.StreakInfo
import com.pgstorm148.heyum.model.RejectedDish
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "heyum_preferences")

class AndroidPreferenceRepository(private val context: Context) : PreferenceRepository {
    private val gson = Gson()
    
    private val REJECTED_DISHES_KEY = stringPreferencesKey("rejected_dishes")
    private val CURRENT_STREAK_KEY = intPreferencesKey("current_streak")
    private val TOTAL_MEALS_KEY = intPreferencesKey("total_meals_logged")
    private val UNIQUE_CUISINES_KEY = stringPreferencesKey("unique_cuisines_tried")

    override suspend fun getRejectedDishes(): Map<String, RejectedDish> {
        val prefs = context.dataStore.data.first()
        val json = prefs[REJECTED_DISHES_KEY] ?: return emptyMap()
        val type = object : TypeToken<Map<String, RejectedDish>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override suspend fun saveRejectedDishes(dishes: Map<String, RejectedDish>) {
        val json = gson.toJson(dishes)
        context.dataStore.edit { prefs ->
            prefs[REJECTED_DISHES_KEY] = json
        }
    }

    override suspend fun getStreakInfo(): StreakInfo {
        val prefs = context.dataStore.data.first()
        val currentStreak = prefs[CURRENT_STREAK_KEY] ?: 0
        val totalMeals = prefs[TOTAL_MEALS_KEY] ?: 0
        val cuisinesJson = prefs[UNIQUE_CUISINES_KEY] ?: "[]"
        val type = object : TypeToken<List<String>>() {}.type
        val cuisines: List<String> = try {
            gson.fromJson(cuisinesJson, type)
        } catch (e: Exception) {
            emptyList()
        }
        
        return StreakInfo(
            currentStreak = currentStreak,
            totalMealsLogged = totalMeals,
            uniqueCuisinesTried = cuisines
        )
    }

    override suspend fun saveStreakInfo(streakInfo: StreakInfo) {
        val json = gson.toJson(streakInfo.uniqueCuisinesTried)
        context.dataStore.edit { prefs ->
            prefs[CURRENT_STREAK_KEY] = streakInfo.currentStreak
            prefs[TOTAL_MEALS_KEY] = streakInfo.totalMealsLogged
            prefs[UNIQUE_CUISINES_KEY] = json
        }
    }

    override suspend fun clearPreferences() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
