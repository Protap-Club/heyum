package com.pgstorm148.heyum.api

import com.pgstorm148.heyum.model.*

data class ScanFridgeRequest(
    val imageBase64: String,
    val userId: String
)

data class ScanFridgeResponse(
    val ingredients: List<String>,
    val possibleDishes: List<Dish>
)

class FridgeScannerAPI {
    /**
     * Simulates a POST /scan-fridge Firebase Cloud Function API request.
     * In V2, this will send the image base64 to the server to process using GPT-4 Vision API.
     * For now, it returns a realistic mock scan mapping matching ingredients and possible dishes.
     */
    suspend fun scanFridge(imageBase64: String, userId: String): ScanFridgeResponse {
        // In a real implementation:
        // val response = httpClient.post("https://firebase-url/scan-fridge") { setBody(ScanFridgeRequest(imageBase64, userId)) }
        // return response.body()

        // Mock response
        val mockIngredients = listOf("Paneer", "Spinach", "Tomato", "Ginger", "Lemon")
        
        val possibleDishes = listOf(
            Dish(
                id = "dish_palak_paneer",
                name = "Fresh Palak Paneer",
                cuisine = Cuisine.INDIAN,
                description = "Creamy pureed spinach curry with soft paneer cubes.",
                spiceLevel = 2,
                prepTimeMinutes = 20,
                difficulty = Difficulty.EASY,
                ingredients = listOf("Paneer", "Spinach", "Tomato", "Ginger", "Coriander"),
                dietaryTags = listOf(DietaryRestriction.VEG, DietaryRestriction.GLUTEN_FREE),
                occasions = listOf("comfort food", "fresh lunch")
            ),
            Dish(
                id = "dish_ginger_tea",
                name = "Warm Ginger Lemon Brew",
                cuisine = Cuisine.COMFORT,
                description = "A soothing herbal beverage to warm your throat and calm the stomach.",
                spiceLevel = 1,
                prepTimeMinutes = 5,
                difficulty = Difficulty.EASY,
                ingredients = listOf("Ginger", "Lemon", "Honey"),
                dietaryTags = listOf(DietaryRestriction.VEGAN, DietaryRestriction.GLUTEN_FREE, DietaryRestriction.DAIRY_FREE),
                occasions = listOf("calming tea", "rainy day")
            )
        )

        return ScanFridgeResponse(
            ingredients = mockIngredients,
            possibleDishes = possibleDishes
        )
    }
}
