package com.pgstorm148.heyum.api

import com.pgstorm148.heyum.engine.TasteProfileEngine
import com.pgstorm148.heyum.firebase.DishRepository
import com.pgstorm148.heyum.firebase.UserRepository
import com.pgstorm148.heyum.model.*
import kotlinx.coroutines.flow.firstOrNull

data class SuggestionResponse(
    val dishes: List<Dish>,
    val reason: String
)

class SuggestionEngineClient(
    private val userRepository: UserRepository,
    private val dishRepository: DishRepository
) {
    /**
     * Simulates calling the Firebase Cloud Function POST /suggest.
     * Integrates with the local TasteProfileEngine as a fallback or for client-side processing.
     */
    suspend fun getSuggestion(userId: String, context: MealContext): SuggestionResponse {
        // Fetch user profile from DB (or create default)
        val user = userRepository.getUserProfile(userId).firstOrNull() 
            ?: UserProfile(id = userId, name = "Yum User")

        // Fetch candidate dishes
        var allDishes = dishRepository.getDishesByContext(null, context.timeOfDay, context.weather).firstOrNull() 
            ?: getFallbackDishPool()
            
        // Filter out rejected dishes and non-matching meal types
        allDishes = allDishes.filter { dish ->
            !context.rejectedDishIds.contains(dish.id) &&
            (context.mealTypeStr == "ALL" || dish.mealType.contains(context.mealTypeStr))
        }

        // Get recommendations
        val recommended = TasteProfileEngine.recommendDishes(user, allDishes, context)

        if (recommended.isEmpty()) {
            return SuggestionResponse(
                dishes = getFallbackDishPool().take(2),
                reason = "Here are some popular warm options to help you settle in."
            )
        }

        val topDish = recommended.first()
        val reason = generateCalmingReason(user, topDish, context)

        return SuggestionResponse(
            dishes = recommended,
            reason = reason
        )
    }

    private fun generateCalmingReason(user: UserProfile, dish: Dish, context: MealContext): String {
        val cuisineName = dish.cuisine.name.lowercase().replace("_", " ")
        
        return when {
            user.favoriteCuisines.contains(dish.cuisine) -> {
                "You'll love this — it's your favorite $cuisineName comfort food for a calming ${context.timeOfDay.name.lowercase()}."
            }
            context.weather.lowercase().contains("rain") -> {
                "Perfect for this rainy day — warm, comforting, and freshly prepared."
            }
            context.weather.lowercase().contains("cold") -> {
                "Something warm to heat up this chilly day."
            }
            dish.spiceLevel >= 4 -> {
                "A little spice to brighten up your mood."
            }
            dish.prepTimeMinutes <= 15 -> {
                "A quick, stress-free meal that is easy on the mind and body."
            }
            else -> {
                "A fresh and wholesome dish selected to nourish you this ${context.timeOfDay.name.lowercase()}."
            }
        }
    }

    private fun getFallbackDishPool(): List<Dish> {
        return getDefaultDishPool()
    }
}
