package com.pgstorm148.heyum.engine

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class KitchenStorage actual constructor() : KoinComponent {
    private val context: Context by inject()
    private val prefs by lazy { context.getSharedPreferences("yum_kitchen_storage", Context.MODE_PRIVATE) }

    actual fun saveIngredients(list: List<String>) {
        prefs.edit().putString("ingredients", list.joinToString("|||")).apply()
    }

    actual fun getIngredients(): List<String> {
        val str = prefs.getString("ingredients", "") ?: ""
        return if (str.isEmpty()) emptyList() else str.split("|||")
    }

    actual fun saveRecentIngredients(list: List<String>) {
        prefs.edit().putString("recent_ingredients", list.joinToString("|||")).apply()
    }

    actual fun getRecentIngredients(): List<String> {
        val str = prefs.getString("recent_ingredients", "") ?: ""
        return if (str.isEmpty()) emptyList() else str.split("|||")
    }
}
