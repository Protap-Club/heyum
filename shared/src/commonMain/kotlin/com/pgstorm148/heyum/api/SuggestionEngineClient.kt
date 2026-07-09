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

        // Fetch candidate dishes. Fall back to the built-in catalog if Firestore returns a
        // thin/empty list, so users always get a full, varied set to browse.
        val remoteDishes = dishRepository.getDishesByContext(null, context.timeOfDay, context.weather).firstOrNull()
        var allDishes = if ((remoteDishes?.size ?: 0) >= 10) remoteDishes!! else getFallbackDishPool()

        // Keep dishes that aren't currently rejected and that fit this meal. A dish with no
        // meal-type tags of its own is treated as suitable for any meal (most of the catalog
        // is untagged, so tagging it out here would wrongly leave nothing to show).
        allDishes = allDishes.filter { dish ->
            !context.rejectedDishIds.contains(dish.id) &&
                (context.mealTypeStr == "ALL" || dish.mealType.isEmpty() || dish.mealType.contains(context.mealTypeStr))
        }

        if (allDishes.isEmpty()) {
            return SuggestionResponse(
                dishes = getFallbackDishPool().shuffled().take(15),
                reason = "Here are some popular warm options to help you settle in."
            )
        }

        // Surface favourite-cuisine dishes first, but include everything (shuffled within each
        // tier) so the deck stays varied and can keep revolving without running dry.
        val (favourites, others) = allDishes.partition { user.favoriteCuisines.contains(it.cuisine) }
        val recommended = (favourites.shuffled() + others.shuffled()).take(20)

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
