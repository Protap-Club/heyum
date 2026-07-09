package com.pgstorm148.heyum.model

data class GroupSession(
    val id: String = "",
    val organizerId: String = "",
    val organizerName: String = "",
    val sessionName: String = "",
    val createdAt: Long = 0,
    val mealType: String = "LUNCH",
    val members: List<GroupMember> = emptyList(),
    val selectedCuisines: Map<String, String> = emptyMap(), // userId -> cuisine
    val selectedDishes: Map<String, String> = emptyMap(), // userId -> typed dish text
    val status: String = "WAITING", // WAITING, ACTIVE, COMPLETED, ENDED
    val finalDish: String = "",
    val joinCode: String = ""
)

data class GroupMember(
    val userId: String = "",
    val name: String = "",
    val tasteProfile: String = "", // Indian, Continental, Italian, etc.
    val joinedAt: Long = 0,
    val isOrganizer: Boolean = false,
    val dietaryRestrictions: List<String> = emptyList(),
    val spiceTolerance: Int = 3,
    val favoriteCuisines: List<String> = emptyList()
)

data class GroupSessionUpdate(
    val memberId: String = "",
    val memberName: String = "",
    val selectedCuisine: String = "",
    val selectedDish: String = "",
    val timestamp: Long = 0
)
