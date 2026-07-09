package com.pgstorm148.heyum.engine

import com.pgstorm148.heyum.firebase.DishRepository
import com.pgstorm148.heyum.firebase.UserRepository
import com.pgstorm148.heyum.model.*
import com.pgstorm148.heyum.ui.screens.nowMillis
import com.pgstorm148.heyum.ui.screens.getCurrentHour
import kotlinx.coroutines.flow.firstOrNull

class WidgetSuggestionEngine(
    private val widgetCache: WidgetCache,
    private val dishRepository: DishRepository,
    private val userRepository: UserRepository
) {
    suspend fun getOrUpdateSuggestion(userId: String): Dish {
        val cached = widgetCache.getCachedDish()
        if (cached != null) return cached

        return refreshSuggestion(userId)
    }

    suspend fun refreshSuggestion(userId: String): Dish {
        widgetCache.clearCache()

        // Fetch user profile and context details
        val profile = userRepository.getUserProfile(userId).firstOrNull() ?: UserProfile(id = userId, name = "User")
        
        // Match suggestion based on current hour
        val hour = getCurrentHour()
        val timeOfDay = when {
            hour in 6..11 -> TimeOfDay.MORNING
            hour in 12..16 -> TimeOfDay.AFTERNOON
            hour in 17..21 -> TimeOfDay.EVENING
            else -> TimeOfDay.LATE_NIGHT
        }

        // Fetch target dishes
        val dishes = dishRepository.getDishesByContext(null, timeOfDay, "sunny").firstOrNull() ?: emptyList()
        
        // Populate dishes if empty
        val dishesPool = if (dishes.isEmpty()) {
            listOf(
                Dish(
                    id = "palak_paneer",
                    name = "Palak Paneer",
                    cuisine = Cuisine.INDIAN,
                    description = "Creamy spinach gravy with cottage cheese.",
                    ingredients = listOf("Spinach", "Paneer")
                ),
                Dish(
                    id = "ginger_tea",
                    name = "Ginger Cardamom Chai",
                    cuisine = Cuisine.COMFORT,
                    description = "Warm tea with cardamom and ginger.",
                    ingredients = listOf("Tea leaves", "Milk", "Ginger")
                ),
                Dish(
                    id = "classic_minestrone",
                    name = "Classic Minestrone",
                    cuisine = Cuisine.HEALTHY,
                    description = "Hearty Italian vegetable soup.",
                    ingredients = listOf("Tomato", "Beans")
                )
            )
        } else {
            dishes
        }

        val context = MealContext(timeOfDay = timeOfDay, weather = "sunny", mood = Mood.COMFORT)

        // Simple scoring - use first dish from pool
        val topDish = dishesPool.firstOrNull() ?: Dish(
            id = "ginger_tea",
            name = "Ginger Cardamom Chai",
            cuisine = Cuisine.COMFORT,
            description = "Warm tea with cardamom and ginger.",
            ingredients = listOf("Tea leaves", "Milk", "Ginger")
        )

        widgetCache.saveCachedDish(topDish)
        triggerWidgetUpdate()
        return topDish
    }

    suspend fun recordUserAction(userId: String, accepted: Boolean) {
        val currentDish = widgetCache.getCachedDish() ?: return
        
        // Log choice in meal history
        val profile = userRepository.getUserProfile(userId).firstOrNull() ?: return

        // Log the action (simplified - just track in history)
        val mealEntry = MealHistoryEntry(
            id = currentDish.id,
            dishName = currentDish.name,
            cuisine = currentDish.cuisine,
            accepted = accepted,
            timestamp = nowMillis(),
            context = MealContext(TimeOfDay.EVENING, "sunny", Mood.COMFORT)
        )

        // Update user meal history only
        userRepository.updateMealHistory(userId, mealEntry)

        // Clear and force update on next render
        widgetCache.clearCache()
        triggerWidgetUpdate()
    }
}

expect fun triggerWidgetUpdate()
