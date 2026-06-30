package com.pgstorm148.heyum.engine

actual class KitchenStorage actual constructor() {
    private var inMemoryIngredients = emptyList<String>()
    private var inMemoryRecent = emptyList<String>()

    actual fun saveIngredients(list: List<String>) {
        inMemoryIngredients = list
    }

    actual fun getIngredients(): List<String> {
        return inMemoryIngredients
    }

    actual fun saveRecentIngredients(list: List<String>) {
        inMemoryRecent = list
    }

    actual fun getRecentIngredients(): List<String> {
        return inMemoryRecent
    }
}
