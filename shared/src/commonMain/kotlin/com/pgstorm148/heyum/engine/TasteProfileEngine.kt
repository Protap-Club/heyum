package com.pgstorm148.heyum.engine

import com.pgstorm148.heyum.model.*

object TasteProfileEngine {

    /**
     * Scores a candidate dish for an entire group session: hard-excludes dishes that violate
     * any member's dietary restriction, then blends each member's long-term taste profile with
     * what they explicitly picked for *this* session (selected cuisine + typed dish text).
     */
    fun scoreDishForGroup(session: GroupSession, dish: Dish): GroupScoreResult {
        val members = session.members
        if (members.isEmpty()) return GroupScoreResult(1.0f, true)

        var totalScore = 0.0f
        var callout: String? = null

        for (member in members) {
            for (restrictionName in member.dietaryRestrictions) {
                val restriction = runCatching { DietaryRestriction.valueOf(restrictionName) }.getOrNull() ?: continue
                if (!isDietCompatible(restriction, dish)) {
                    return GroupScoreResult(0.0f, false, "${member.name} can't eat this (${restriction.name.lowercase()}) 🥬")
                }
            }

            var memberScore = 0.5f

            if (dish.spiceLevel > member.spiceTolerance) {
                memberScore -= 0.15f
                if (callout == null) callout = "${member.name} doesn't do spicy 🔥"
            }

            if (member.favoriteCuisines.contains(dish.cuisine.name)) {
                memberScore += 0.1f
            }

            val typedCuisine = session.selectedCuisines[member.userId]
            if (!typedCuisine.isNullOrBlank() && typedCuisine.equals(dish.cuisine.name, ignoreCase = true)) {
                memberScore += 0.25f
            }

            val typedDish = session.selectedDishes[member.userId]
            if (!typedDish.isNullOrBlank()) {
                memberScore += dishTextMatchScore(typedDish, dish) * 0.35f
            }

            totalScore += memberScore.coerceIn(0.0f, 1.0f)
        }

        val avgScore = totalScore / members.size
        return GroupScoreResult(avgScore, true, callout)
    }

    /**
     * Picks the single best-matching dish for everyone in the session out of a candidate pool.
     * Candidates are first narrowed to the session's meal type (dishes with no meal-type tags
     * of their own are treated as suitable for any meal, since the seed catalog rarely sets them).
     */
    fun findGroupMatch(session: GroupSession, candidates: List<Dish>): Pair<Dish, GroupScoreResult>? {
        return candidates
            .filter { it.mealType.isEmpty() || it.mealType.contains(session.mealType) }
            .map { dish -> dish to scoreDishForGroup(session, dish) }
            .filter { it.second.isCompatible }
            .maxByOrNull { it.second.score }
    }

    private fun isDietCompatible(restriction: DietaryRestriction, dish: Dish): Boolean {
        return when (restriction) {
            DietaryRestriction.VEG -> dish.dietaryTags.contains(DietaryRestriction.VEG) ||
                dish.dietaryTags.contains(DietaryRestriction.VEGAN) ||
                dish.dietaryTags.contains(DietaryRestriction.JAIN)
            DietaryRestriction.VEGAN -> dish.dietaryTags.contains(DietaryRestriction.VEGAN)
            DietaryRestriction.JAIN -> {
                val rootVegetables = listOf("onion", "garlic", "potato", "carrot", "radish", "ginger", "beetroot")
                dish.dietaryTags.contains(DietaryRestriction.JAIN) &&
                    dish.ingredients.none { ingredient -> rootVegetables.any { ingredient.lowercase().contains(it) } }
            }
            DietaryRestriction.GLUTEN_FREE -> dish.dietaryTags.contains(DietaryRestriction.GLUTEN_FREE)
            DietaryRestriction.DAIRY_FREE -> dish.dietaryTags.contains(DietaryRestriction.DAIRY_FREE)
            DietaryRestriction.NON_VEG, DietaryRestriction.NONE -> true
        }
    }

    /** Word-overlap match between what a member typed and a candidate dish's name/description/ingredients. */
    private fun dishTextMatchScore(typed: String, dish: Dish): Float {
        val query = typed.trim().lowercase()
        if (query.isEmpty()) return 0f
        val haystack = (listOf(dish.name, dish.description) + dish.ingredients).joinToString(" ").lowercase()
        if (haystack.contains(query)) return 1.0f

        val queryWords = query.split(Regex("\\s+")).filter { it.length > 2 }.toSet()
        if (queryWords.isEmpty()) return 0f
        val haystackWords = haystack.split(Regex("\\s+")).toSet()
        val overlap = queryWords.count { word -> haystackWords.any { it.contains(word) } }
        return overlap.toFloat() / queryWords.size
    }
}

data class GroupScoreResult(
    val score: Float,
    val isCompatible: Boolean,
    val callout: String? = null
)
