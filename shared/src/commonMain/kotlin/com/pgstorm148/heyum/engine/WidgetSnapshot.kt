package com.pgstorm148.heyum.engine

/** A single craving the user logged, trimmed to just what the home-screen widget renders. */
data class WidgetCraving(
    val foodName: String,
    val mood: String,
    val timestamp: Long
)

/** A meal the user actually logged eating, trimmed for widget rendering. */
data class WidgetMeal(
    val dishName: String,
    val mealType: String,
    val timestamp: Long
)

/**
 * Everything the home-screen widget needs, in one flat object.
 *
 * Widgets render in a different process (Android) or a separate extension binary (iOS), so they
 * can't reach Firestore, Koin, or the app's repositories. Instead the app writes this snapshot to
 * shared storage whenever the underlying data changes, and the widget only ever reads it.
 */
data class WidgetSnapshot(
    val cravings: List<WidgetCraving> = emptyList(),
    val meals: List<WidgetMeal> = emptyList(),
    /** Join code of the group session in progress, or null when there isn't one. */
    val sessionCode: String? = null,
    val sessionName: String? = null
)

/** The three panes the widget can show, in cycle order. */
enum class WidgetPane { CRAVINGS, MEALS, SESSION }

/**
 * Shared storage for [WidgetSnapshot], readable from the widget process/extension.
 *
 * Android backs this with SharedPreferences; iOS uses an App Group container, which is the only
 * way an app and its widget extension can see the same defaults.
 */
expect class WidgetDataStore() {
    fun save(snapshot: WidgetSnapshot)
    fun load(): WidgetSnapshot
    /** Index of the pane the small widget is currently showing; persists across re-renders. */
    fun getPaneIndex(): Int
    fun savePaneIndex(index: Int)
}

/** Caps stored entries so the widget stays cheap to read and render. */
const val WIDGET_MAX_CRAVINGS = 5
const val WIDGET_MAX_MEALS = 5
