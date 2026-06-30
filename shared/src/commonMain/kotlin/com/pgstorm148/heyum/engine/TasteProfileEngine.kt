package com.pgstorm148.heyum.engine

import com.pgstorm148.heyum.model.*
import kotlin.math.abs

object TasteProfileEngine {

    fun scoreDishForGroup(members: List<GroupMember>, dish: Dish): GroupScoreResult {
        if (members.isEmpty()) return GroupScoreResult(1.0f, true)

        var totalScore = 0.0f
        var callout: String? = null
        var isCompatible = true

        for (member in members) {
            // 1. Dietary check
            var memberDietCompatible = true
            for (restriction in member.dietaryRestrictions) {
                if (restriction == DietaryRestriction.NONE) continue
                if (restriction == DietaryRestriction.VEG) {
                    if (!dish.dietaryTags.contains(DietaryRestriction.VEG) && 
                        !dish.dietaryTags.contains(DietaryRestriction.VEGAN) &&
                        !dish.dietaryTags.contains(DietaryRestriction.JAIN)
                    ) {
                        memberDietCompatible = false
                    }
                } else if (restriction == DietaryRestriction.VEGAN) {
                    if (!dish.dietaryTags.contains(DietaryRestriction.VEGAN)) {
                        memberDietCompatible = false
                    }
                } else if (restriction == DietaryRestriction.JAIN) {
                    if (!dish.dietaryTags.contains(DietaryRestriction.JAIN)) {
                        memberDietCompatible = false
                    }
                    val rootVegetables = listOf("onion", "garlic", "potato", "carrot", "radish", "ginger", "beetroot")
                    if (dish.ingredients.any { ingredient -> 
                            rootVegetables.any { root -> ingredient.lowercase().contains(root) } 
                        }) {
                        memberDietCompatible = false
                    }
                }
            }

            if (!memberDietCompatible) {
                isCompatible = false
                callout = "${member.name} has strict dietary limits 🥬"
                break
            }

            // 2. Spice check
            var memberScore = 0.7f
            if (dish.spiceLevel > member.spiceTolerance) {
                memberScore -= 0.3f
                if (callout == null) {
                    callout = "${member.name} doesn't do spicy 🔥"
                }
            }

            // 3. Cuisine check
            if (member.favoriteCuisines.contains(dish.cuisine)) {
                memberScore += 0.2f
            }

            totalScore += memberScore.coerceIn(0.0f, 1.0f)
        }

        if (!isCompatible) {
            return GroupScoreResult(0.0f, false, callout)
        }

        val avgScore = totalScore / members.size
        return GroupScoreResult(avgScore, true, callout)
    }

    /**
     * Scores a single dish for a user in a given context.
     * Returns a score between 0.0 and 1.0.
     */
    fun scoreDish(user: UserProfile, dish: Dish, context: MealContext): Float {
        // Completely exclude dishes already eaten today
        if (context.todayDishIds.contains(dish.id)) {
            return 0.0f
        }

        // 1. Dietary Compatibility (20%)
        val dietaryScore = calculateDietaryCompatibility(user, dish)
        if (dietaryScore == 0.0f) {
            return 0.0f // Incompatible meals should not be recommended
        }

        // Check if user is new (cold start)
        val isNewUser = user.mealHistory.isEmpty() && user.tasteVector.isEmpty()

        // 2. Taste Vector Match (40%)
        val tasteScore = if (isNewUser) {
            // New user fallback: match with favorite cuisines
            if (user.favoriteCuisines.contains(dish.cuisine)) 0.8f else 0.5f
        } else {
            calculateTasteVectorScore(user, dish)
        }

        // 3. Context Match (25%)
        val contextScore = calculateContextScore(dish, context)

        // 4. Variety Bonus (15%)
        val varietyScore = if (isNewUser) calculateVarietyScore(user, dish, context) else calculateVarietyScore(user, dish, context)

        // Weighted sum
        return (tasteScore * 0.40f) + (contextScore * 0.25f) + (dietaryScore * 0.20f) + (varietyScore * 0.15f)
    }

    /**
     * Recommends the top 5 scored dishes out of a pool of candidates.
     */
    fun recommendDishes(user: UserProfile, dishes: List<Dish>, context: MealContext): List<Dish> {
        if (dishes.isEmpty()) return emptyList()

        return dishes
            .map { dish -> dish to scoreDish(user, dish, context) }
            .filter { it.second > 0.0f } // Filter out fully incompatible dishes
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
    }

    /**
     * Updates the user's taste vector and meal history after accepting or rejecting a dish.
     */
    fun updateTasteProfile(
        user: UserProfile,
        dish: Dish,
        accepted: Boolean,
        context: MealContext,
        timestamp: Long
    ): UserProfile {
        // Extract all tags for the dish
        val tags = getDishTags(dish)
        val currentVector = user.tasteVector.toMutableMap()

        val delta = if (accepted) 0.1f else -0.1f

        for (tag in tags) {
            val currentWeight = currentVector[tag] ?: 0.0f
            val newWeight = (currentWeight + delta).coerceIn(-1.0f, 1.0f)
            currentVector[tag] = newWeight
        }

        // Add to history
        val newHistoryEntry = MealHistoryEntry(
            id = "hist_${timestamp}_${dish.id}",
            dishName = dish.name,
            cuisine = dish.cuisine,
            accepted = accepted,
            timestamp = timestamp,
            context = context
        )

        return user.copy(
            tasteVector = currentVector,
            mealHistory = user.mealHistory + newHistoryEntry
        )
    }

    // --- Private Helper Functions ---

    private fun getDishTags(dish: Dish): List<String> {
        val tags = mutableListOf<String>()
        tags.add("cuisine_${dish.cuisine.name.lowercase()}")
        tags.add("spice_${dish.spiceLevel}")
        tags.add("difficulty_${dish.difficulty.name.lowercase()}")
        
        dish.dietaryTags.forEach { tag ->
            tags.add("diet_${tag.name.lowercase()}")
        }
        
        dish.occasions.forEach { occasion ->
            tags.add("occasion_${occasion.lowercase().trim()}")
        }
        
        dish.ingredients.forEach { ingredient ->
            tags.add("ing_${ingredient.lowercase().trim()}")
        }

        return tags
    }

    private fun calculateDietaryCompatibility(user: UserProfile, dish: Dish): Float {
        // Check for disliked ingredients
        for (disliked in user.dislikedIngredients) {
            val dislikedClean = disliked.lowercase().trim()
            if (dish.ingredients.any { it.lowercase().contains(dislikedClean) }) {
                return 0.0f // Incompatible
            }
        }

        if (user.dietaryRestrictions.isEmpty() || user.dietaryRestrictions.contains(DietaryRestriction.NONE)) {
            return 1.0f
        }

        // Validate each restriction
        for (restriction in user.dietaryRestrictions) {
            when (restriction) {
                DietaryRestriction.VEG -> {
                    if (!dish.dietaryTags.contains(DietaryRestriction.VEG) && 
                        !dish.dietaryTags.contains(DietaryRestriction.VEGAN) &&
                        !dish.dietaryTags.contains(DietaryRestriction.JAIN)
                    ) {
                        return 0.0f
                    }
                }
                DietaryRestriction.VEGAN -> {
                    if (!dish.dietaryTags.contains(DietaryRestriction.VEGAN)) {
                        return 0.0f
                    }
                }
                DietaryRestriction.JAIN -> {
                    if (!dish.dietaryTags.contains(DietaryRestriction.JAIN)) {
                        return 0.0f
                    }
                    // Additional check: Jains don't eat root vegetables (onion, garlic, potato, carrot, etc.)
                    val rootVegetables = listOf("onion", "garlic", "potato", "carrot", "radish", "ginger", "beetroot")
                    if (dish.ingredients.any { ingredient -> 
                            rootVegetables.any { root -> ingredient.lowercase().contains(root) } 
                        }) {
                        return 0.0f
                    }
                }
                DietaryRestriction.GLUTEN_FREE -> {
                    if (!dish.dietaryTags.contains(DietaryRestriction.GLUTEN_FREE)) {
                        return 0.0f
                    }
                }
                DietaryRestriction.DAIRY_FREE -> {
                    if (!dish.dietaryTags.contains(DietaryRestriction.DAIRY_FREE)) {
                        return 0.0f
                    }
                }
                else -> {}
            }
        }

        return 1.0f
    }

    private fun calculateTasteVectorScore(user: UserProfile, dish: Dish): Float {
        val tags = getDishTags(dish)
        if (tags.isEmpty()) return 0.5f

        var sumWeights = 0.0f
        var matchCount = 0

        for (tag in tags) {
            val weight = user.tasteVector[tag]
            if (weight != null) {
                sumWeights += weight
                matchCount++
            }
        }

        if (matchCount == 0) return 0.5f // Neutral score for no overlap

        val avg = sumWeights / matchCount
        // Normalize from [-1.0, 1.0] to [0.0, 1.0]
        return (avg + 1.0f) / 2.0f
    }

    private fun calculateContextScore(dish: Dish, context: MealContext): Float {
        var score = 0.0f
        var factors = 0

        // 1. Time of Day Match
        val timeScore = when (context.timeOfDay) {
            TimeOfDay.MORNING -> {
                // Breakfast matches quick/easy meals, teas, or occasions containing "breakfast" or "morning"
                if (dish.prepTimeMinutes <= 20 || dish.occasions.any { it.contains("breakfast") || it.contains("morning") }) 1.0f else 0.4f
            }
            TimeOfDay.AFTERNOON -> {
                // Lunch matches filling/main course dishes, or occasions containing "lunch" or "afternoon"
                if (dish.prepTimeMinutes > 15 && dish.occasions.any { it.contains("lunch") || it.contains("afternoon") || it.contains("meal") }) 1.0f else 0.7f
            }
            TimeOfDay.EVENING -> {
                // Dinner matches diverse cuisines, occasions containing "dinner"
                if (dish.occasions.any { it.contains("dinner") || it.contains("evening") || it.contains("party") }) 1.0f else 0.7f
            }
            TimeOfDay.LATE_NIGHT -> {
                // Late night matches light snacks, comfort food, quick meals, or beverages (tea)
                if (dish.prepTimeMinutes <= 15 || dish.occasions.any { it.contains("midnight") || it.contains("comfort") || it.contains("tea") }) 1.0f else 0.3f
            }
        }
        score += timeScore
        factors++

        // 2. Weather Match
        val weatherClean = context.weather.lowercase().trim()
        val weatherScore = if (weatherClean.contains("rain") || weatherClean.contains("storm")) {
            // Rainy weather matches comfort food, hot tea, fried/street food
            if (dish.occasions.any { it.contains("rain") || it.contains("comfort") } || dish.cuisine == Cuisine.STREET_FOOD) 1.0f else 0.5f
        } else if (weatherClean.contains("cold") || weatherClean.contains("winter") || weatherClean.contains("snow")) {
            // Cold weather matches soup, hot meals, spicy dishes
            if (dish.occasions.any { it.contains("soup") || it.contains("cozy") || it.contains("comfort") } || dish.spiceLevel >= 3) 1.0f else 0.5f
        } else if (weatherClean.contains("hot") || weatherClean.contains("summer") || weatherClean.contains("warm")) {
            // Hot weather matches light, healthy, salads, juices, low prep time
            if (dish.occasions.any { it.contains("light") || it.contains("summer") } || dish.cuisine == Cuisine.HEALTHY || dish.prepTimeMinutes < 20) 1.0f else 0.4f
        } else {
            1.0f // Neutral/ideal weather
        }
        score += weatherScore
        factors++

        // 3. Mood Match
        val mood = context.mood
        if (mood != null) {
            val moodScore = when (mood) {
                Mood.COMFORT -> if (dish.occasions.any { it.contains("comfort") } || dish.cuisine == Cuisine.COMFORT) 1.0f else 0.5f
                Mood.LIGHT -> if (dish.cuisine == Cuisine.HEALTHY || dish.dietaryTags.contains(DietaryRestriction.VEGAN) || dish.prepTimeMinutes <= 15) 1.0f else 0.4f
                Mood.QUICK -> if (dish.prepTimeMinutes <= 15 || dish.difficulty == Difficulty.EASY) 1.0f else 0.3f
                Mood.SPICY -> if (dish.spiceLevel >= 4) 1.0f else 0.2f
                Mood.ADVENTUROUS -> if (dish.difficulty == Difficulty.HARD || dish.cuisine == Cuisine.STREET_FOOD) 1.0f else 0.5f
            }
            score += moodScore
            factors++
        }

        return score / factors
    }

    private fun calculateVarietyScore(user: UserProfile, dish: Dish, context: MealContext): Float {
        var baseScore = 1.0f
        
        // Penalize repetition in today's cuisines
        if (context.todayCuisines.contains(dish.cuisine.name)) {
            baseScore -= 0.4f
        }
        
        // Penalize heavy dinners if they had a heavy lunch
        if (context.hadHeavyLunch && context.mealTypeStr == "Dinner" && (dish.cuisine == Cuisine.INDIAN || dish.cuisine == Cuisine.COMFORT)) {
            baseScore -= 0.5f
        }

        val history = user.mealHistory
        if (history.isEmpty()) return baseScore.coerceIn(0.0f, 1.0f)

        // Penalize recent accepts of the SAME dish
        var penalty = 0.0f
        val recentCount = 5
        val recentHistory = history.takeLast(recentCount)

        recentHistory.forEachIndexed { index, entry ->
            if (entry.dishName.lowercase() == dish.name.lowercase() && entry.accepted) {
                // Higher penalty if it was eaten very recently (closer to the end of list)
                val recencyFactor = (index + 1).toFloat() / recentCount // 0.2 to 1.0
                penalty += recencyFactor * 0.5f
            }
        }

        // Reward novelty: if user has not eaten this cuisine in the last 10 history entries, give a small boost
        val cuisineRecent = history.takeLast(10).any { it.cuisine == dish.cuisine && it.accepted }
        val noveltyBonus = if (!cuisineRecent) 0.3f else 0.0f

        return (baseScore - penalty + noveltyBonus).coerceIn(0.0f, 1.0f)
    }
}

data class GroupScoreResult(
    val score: Float,
    val isCompatible: Boolean,
    val callout: String? = null
)
