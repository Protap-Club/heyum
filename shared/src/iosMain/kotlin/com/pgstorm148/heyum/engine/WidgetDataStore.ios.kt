package com.pgstorm148.heyum.engine

import platform.Foundation.NSUserDefaults

/**
 * App Group shared with the widget extension. A widget runs as a separate binary, so
 * NSUserDefaults.standardUserDefaults is NOT visible to it — only a suite backed by an App Group
 * container is. This identifier must match the App Group enabled on both targets in the Apple
 * Developer portal and in each target's entitlements file.
 */
const val HEYUM_APP_GROUP = "group.com.pgstorm148.heyum.widget"

private const val KEY_CRAVINGS = "widget.cravings"
private const val KEY_MEALS = "widget.meals"
private const val KEY_SESSION_CODE = "widget.sessionCode"
private const val KEY_SESSION_NAME = "widget.sessionName"
private const val KEY_PANE_INDEX = "widget.paneIndex"

actual class WidgetDataStore actual constructor() {
    // Falls back to standard defaults if the App Group isn't provisioned yet, so the app keeps
    // working (the widget just won't see data) instead of crashing on a nil suite.
    private val defaults: NSUserDefaults =
        NSUserDefaults(suiteName = HEYUM_APP_GROUP) ?: NSUserDefaults.standardUserDefaults

    actual fun save(snapshot: WidgetSnapshot) {
        defaults.setObject(
            snapshot.cravings.take(WIDGET_MAX_CRAVINGS).map {
                mapOf("foodName" to it.foodName, "mood" to it.mood, "timestamp" to it.timestamp)
            },
            forKey = KEY_CRAVINGS
        )
        defaults.setObject(
            snapshot.meals.take(WIDGET_MAX_MEALS).map {
                mapOf("dishName" to it.dishName, "mealType" to it.mealType, "timestamp" to it.timestamp)
            },
            forKey = KEY_MEALS
        )
        defaults.setObject(snapshot.sessionCode, forKey = KEY_SESSION_CODE)
        defaults.setObject(snapshot.sessionName, forKey = KEY_SESSION_NAME)
    }

    actual fun load(): WidgetSnapshot {
        val cravings = (defaults.arrayForKey(KEY_CRAVINGS) as? List<*>).orEmpty().mapNotNull { item ->
            val dict = item as? Map<*, *> ?: return@mapNotNull null
            WidgetCraving(
                foodName = dict["foodName"] as? String ?: return@mapNotNull null,
                mood = dict["mood"] as? String ?: "",
                timestamp = (dict["timestamp"] as? Number)?.toLong() ?: 0L
            )
        }

        val meals = (defaults.arrayForKey(KEY_MEALS) as? List<*>).orEmpty().mapNotNull { item ->
            val dict = item as? Map<*, *> ?: return@mapNotNull null
            WidgetMeal(
                dishName = dict["dishName"] as? String ?: return@mapNotNull null,
                mealType = dict["mealType"] as? String ?: "",
                timestamp = (dict["timestamp"] as? Number)?.toLong() ?: 0L
            )
        }

        return WidgetSnapshot(
            cravings = cravings,
            meals = meals,
            sessionCode = (defaults.stringForKey(KEY_SESSION_CODE))?.takeIf { it.isNotBlank() },
            sessionName = (defaults.stringForKey(KEY_SESSION_NAME))?.takeIf { it.isNotBlank() }
        )
    }

    actual fun getPaneIndex(): Int = defaults.integerForKey(KEY_PANE_INDEX).toInt()

    actual fun savePaneIndex(index: Int) {
        defaults.setInteger(index.toLong(), forKey = KEY_PANE_INDEX)
    }
}
