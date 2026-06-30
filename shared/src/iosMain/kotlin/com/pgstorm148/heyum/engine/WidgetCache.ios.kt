package com.pgstorm148.heyum.engine

import com.pgstorm148.heyum.model.Dish

actual class WidgetCache actual constructor() {
    private var inMemoryDish: Dish? = null

    actual fun getCachedDish(): Dish? {
        return inMemoryDish
    }

    actual fun saveCachedDish(dish: Dish) {
        inMemoryDish = dish
    }

    actual fun clearCache() {
        inMemoryDish = null
    }
}

actual fun triggerWidgetUpdate() {
    // No-op for iOS bridge testing
}
