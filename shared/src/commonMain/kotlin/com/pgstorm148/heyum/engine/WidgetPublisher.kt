package com.pgstorm148.heyum.engine

import com.pgstorm148.heyum.model.Craving
import com.pgstorm148.heyum.model.MealLogEntry

/**
 * Single place the app pushes home-screen widget data from.
 *
 * Each publish call merges into the stored snapshot rather than replacing it, so updating one
 * section (say, a new craving) never blanks out the others. Every call re-triggers a widget
 * refresh, which is cheap and keeps the widget from showing stale content after an in-app change.
 */
class WidgetPublisher(private val store: WidgetDataStore) {

    fun publishCravings(cravings: List<Craving>) {
        val snapshot = store.load().copy(
            cravings = cravings
                .sortedByDescending { it.timestamp }
                .take(WIDGET_MAX_CRAVINGS)
                .map { WidgetCraving(it.foodName, it.mood, it.timestamp) }
        )
        saveAndRefresh(snapshot)
    }

    fun publishMeals(meals: List<MealLogEntry>) {
        val snapshot = store.load().copy(
            meals = meals
                .sortedByDescending { it.timestamp }
                .take(WIDGET_MAX_MEALS)
                .map { WidgetMeal(it.dishName, it.mealType, it.timestamp) }
        )
        saveAndRefresh(snapshot)
    }

    /** Called while a group session is live so the widget can offer a one-tap jump back into it. */
    fun publishSession(joinCode: String, sessionName: String) {
        val snapshot = store.load().copy(sessionCode = joinCode, sessionName = sessionName)
        saveAndRefresh(snapshot)
    }

    /** Called when the session ends or the user leaves, so the widget stops advertising it. */
    fun clearSession() {
        val snapshot = store.load().copy(sessionCode = null, sessionName = null)
        saveAndRefresh(snapshot)
    }

    private fun saveAndRefresh(snapshot: WidgetSnapshot) {
        try {
            store.save(snapshot)
            triggerWidgetUpdate()
        } catch (e: Exception) {
            // The widget is strictly supplementary — never let a refresh failure surface in the app.
            e.printStackTrace()
        }
    }
}
