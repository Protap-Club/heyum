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

/**
 * Set from Swift at startup to `WidgetCenter.shared.reloadAllTimelines`.
 *
 * WidgetKit has no Kotlin/Native bindings, so the reload has to be bounced through Swift the same
 * way the Firebase delegates are. Left null the widget simply refreshes on its own timeline.
 */
object IosWidgetRefreshBridge {
    var reloadTimelines: (() -> Unit)? = null
}

actual fun triggerWidgetUpdate() {
    try {
        IosWidgetRefreshBridge.reloadTimelines?.invoke()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
