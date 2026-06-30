package com.pgstorm148.heyum.engine

import com.pgstorm148.heyum.model.Dish

expect class WidgetCache() {
    fun getCachedDish(): Dish?
    fun saveCachedDish(dish: Dish)
    fun clearCache()
}
