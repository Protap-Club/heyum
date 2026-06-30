package com.pgstorm148.heyum.engine

import android.content.Context
import com.pgstorm148.heyum.model.Cuisine
import com.pgstorm148.heyum.model.Difficulty
import com.pgstorm148.heyum.model.DietaryRestriction
import com.pgstorm148.heyum.model.Dish
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class WidgetCache actual constructor() : KoinComponent {
    private val context: Context by inject()
    private val prefs by lazy { context.getSharedPreferences("yum_widget_cache", Context.MODE_PRIVATE) }

    actual fun getCachedDish(): Dish? {
        val id = prefs.getString("id", null) ?: return null
        val name = prefs.getString("name", "") ?: ""
        val cuisineStr = prefs.getString("cuisine", "COMFORT") ?: "COMFORT"
        val description = prefs.getString("description", "") ?: ""
        val imageUrl = prefs.getString("imageUrl", "") ?: ""
        val spiceLevel = prefs.getInt("spiceLevel", 3)
        val prepTimeMinutes = prefs.getInt("prepTimeMinutes", 30)
        val difficultyStr = prefs.getString("difficulty", "MEDIUM") ?: "MEDIUM"
        val ingredientsStr = prefs.getString("ingredients", "") ?: ""
        val instructionsStr = prefs.getString("instructions", "") ?: ""
        val dietaryTagsStr = prefs.getString("dietaryTags", "") ?: ""
        val occasionsStr = prefs.getString("occasions", "") ?: ""
        val orderingAvailable = prefs.getBoolean("orderingAvailable", false)
        val orderingUrl = prefs.getString("orderingUrl", null)

        val cuisine = try { Cuisine.valueOf(cuisineStr) } catch(e: Exception) { Cuisine.COMFORT }
        val difficulty = try { Difficulty.valueOf(difficultyStr) } catch(e: Exception) { Difficulty.MEDIUM }
        
        val ingredients = if (ingredientsStr.isEmpty()) emptyList() else ingredientsStr.split("|||")
        val instructions = if (instructionsStr.isEmpty()) emptyList() else instructionsStr.split("|||")
        val dietaryTags = if (dietaryTagsStr.isEmpty()) emptyList() else dietaryTagsStr.split("|||").mapNotNull {
            try { DietaryRestriction.valueOf(it) } catch(e: Exception) { null }
        }
        val occasions = if (occasionsStr.isEmpty()) emptyList() else occasionsStr.split("|||")

        return Dish(
            id = id,
            name = name,
            cuisine = cuisine,
            description = description,
            imageUrl = imageUrl,
            spiceLevel = spiceLevel,
            prepTimeMinutes = prepTimeMinutes,
            difficulty = difficulty,
            ingredients = ingredients,
            instructions = instructions,
            dietaryTags = dietaryTags,
            occasions = occasions,
            orderingAvailable = orderingAvailable,
            orderingUrl = orderingUrl
        )
    }

    actual fun saveCachedDish(dish: Dish) {
        prefs.edit().apply {
            putString("id", dish.id)
            putString("name", dish.name)
            putString("cuisine", dish.cuisine.name)
            putString("description", dish.description)
            putString("imageUrl", dish.imageUrl)
            putInt("spiceLevel", dish.spiceLevel)
            putInt("prepTimeMinutes", dish.prepTimeMinutes)
            putString("difficulty", dish.difficulty.name)
            putString("ingredients", dish.ingredients.joinToString("|||"))
            putString("instructions", dish.instructions.joinToString("|||"))
            putString("dietaryTags", dish.dietaryTags.joinToString("|||") { it.name })
            putString("occasions", dish.occasions.joinToString("|||"))
            putBoolean("orderingAvailable", dish.orderingAvailable)
            putString("orderingUrl", dish.orderingUrl)
            apply()
        }
    }

    actual fun clearCache() {
        prefs.edit().clear().apply()
    }
}

actual fun triggerWidgetUpdate() {
    try {
        val context: Context = org.koin.core.context.GlobalContext.get().get()
        val intent = android.content.Intent("com.pgstorm148.heyum.action.UPDATE_WIDGET").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
