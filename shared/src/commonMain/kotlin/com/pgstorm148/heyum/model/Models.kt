package com.pgstorm148.heyum.model

// Existing Auth/Craving Models
data class FirebaseUser(
    val uid: String,
    val email: String?
)

data class Craving(
    val id: String = "",
    val userId: String = "",
    val foodName: String = "",
    val mood: String = "",
    val timestamp: Long = 0L,
    val notes: String = ""
)

data class FoodSuggestion(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val ingredients: List<String>,
    val instructions: List<String>,
    val preparationTime: String,
    val energy: String,
    val calmingNote: String
)

// Taste Profile Enums
enum class DietaryRestriction {
    VEG, NON_VEG, VEGAN, JAIN, GLUTEN_FREE, DAIRY_FREE, NONE
}

enum class Cuisine {
    INDIAN, CHINESE, ITALIAN, MEXICAN, JAPANESE, THAI, MEDITERRANEAN, STREET_FOOD, COMFORT, HEALTHY, FAST_FOOD
}

enum class TimeOfDay {
    MORNING, AFTERNOON, EVENING, LATE_NIGHT
}

enum class Mood {
    COMFORT, LIGHT, QUICK, SPICY, ADVENTUROUS
}

enum class Difficulty {
    EASY, MEDIUM, HARD
}

enum class NotificationFrequency {
    OFTEN, STANDARD, MINIMAL
}

// User Profile
data class UserProfile(
    val id: String,
    val name: String,
    val dietaryRestrictions: List<DietaryRestriction> = emptyList(),
    val spiceTolerance: Int = 3, // 1-5 scale
    val favoriteCuisines: List<Cuisine> = emptyList(),
    val dislikedIngredients: List<String> = emptyList(),
    val mealHistory: List<MealHistoryEntry> = emptyList(),
    val tasteVector: Map<String, Float> = emptyMap(), // Tag -> Weight
    val onboardingCompleted: Boolean = false,
    
    // Notification Settings
    val notificationFrequency: NotificationFrequency = NotificationFrequency.STANDARD,
    val quietHoursStart: String = "23:00",
    val quietHoursEnd: String = "08:00",
    val quietHoursEnabled: Boolean = false,
    val mealRemindersEnabled: Boolean = true,
    val reorderSuggestionsEnabled: Boolean = true,
    val wasteAlertsEnabled: Boolean = true,
    val groupInvitesEnabled: Boolean = true
)

// Meal History
data class MealHistoryEntry(
    val id: String,
    val dishName: String,
    val cuisine: Cuisine,
    val accepted: Boolean,
    val timestamp: Long,
    val context: MealContext
)

// Meal Context
data class MealContext(
    val timeOfDay: TimeOfDay,
    val weather: String, // e.g. "sunny", "rainy", "cold", "hot"
    val mood: Mood?,
    val groupSize: Int = 1, // 1 = solo, 2 = duo, 3+ = group
    val availableIngredients: List<String>? = null,
    val hadHeavyLunch: Boolean = false,
    val todayCuisines: List<String> = emptyList(),
    val todayDishIds: List<String> = emptyList(),
    val mealTypeStr: String = "ALL",
    val rejectedDishIds: List<String> = emptyList()
)

enum class MealType(val displayName: String, val emoji: String, val timeRange: String) {
    BREAKFAST("Breakfast", "🌅", "6 AM - 11 AM"),
    LUNCH("Lunch", "🌞", "11 AM - 3 PM"),
    SNACKS("Evening Snacks", "🌇", "3 PM - 7 PM"),
    DINNER("Dinner", "🌙", "7 PM - 12 AM"),
    LATE_NIGHT("Late Night", "🦉", "12 AM - 6 AM"),
    DESSERT("Dessert", "🍰", "Anytime"),
    ALL("All Day", "🍽️", "Anytime")
}

data class MealLogEntry(
    val id: String = "",
    val dishId: String = "",
    val dishName: String = "",
    val cuisine: String = "",
    val mealType: String = "",
    val timestamp: Long = 0L,
    val accepted: Boolean = true,
    val rating: String? = null
)

data class RejectedDish(
    val dishId: String,
    val rejectedAt: Long,
    val mealType: String
)

// Dish
data class Dish(
    val id: String,
    val name: String,
    val cuisine: Cuisine,
    val description: String,
    val imageUrl: String = "",
    val spiceLevel: Int = 3, // 1-5
    val prepTimeMinutes: Int = 30,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val ingredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val dietaryTags: List<DietaryRestriction> = emptyList(),
    val occasions: List<String> = emptyList(),
    val orderingAvailable: Boolean = false,
    val orderingUrl: String? = null,
    val mealType: List<String> = emptyList()
)
data class NotificationPayload(
    val type: String, // nudge, reminder, reorder, reorder_direct, waste, group
    val dishId: String? = null,
    val groupId: String? = null,
    val ingredient: String? = null
)
