package com.pgstorm148.heyum.engine

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// Entries are stored as delimited strings rather than JSON to avoid pulling a serialization
// dependency into the widget's process, matching how WidgetCache.android.kt already persists.
private const val FIELD_SEP = "|:|"
private const val ENTRY_SEP = "|;|"

actual class WidgetDataStore actual constructor() : KoinComponent {
    private val context: Context by inject()
    private val prefs by lazy { context.getSharedPreferences("heyum_widget_data", Context.MODE_PRIVATE) }

    actual fun save(snapshot: WidgetSnapshot) {
        prefs.edit().apply {
            putString(
                "cravings",
                snapshot.cravings.take(WIDGET_MAX_CRAVINGS).joinToString(ENTRY_SEP) {
                    listOf(it.foodName, it.mood, it.timestamp.toString()).joinToString(FIELD_SEP)
                }
            )
            putString(
                "meals",
                snapshot.meals.take(WIDGET_MAX_MEALS).joinToString(ENTRY_SEP) {
                    listOf(it.dishName, it.mealType, it.timestamp.toString()).joinToString(FIELD_SEP)
                }
            )
            putString("sessionCode", snapshot.sessionCode)
            putString("sessionName", snapshot.sessionName)
            apply()
        }
    }

    actual fun load(): WidgetSnapshot {
        val cravings = (prefs.getString("cravings", "") ?: "")
            .split(ENTRY_SEP)
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split(FIELD_SEP)
                if (parts.size < 3) null
                else WidgetCraving(parts[0], parts[1], parts[2].toLongOrNull() ?: 0L)
            }

        val meals = (prefs.getString("meals", "") ?: "")
            .split(ENTRY_SEP)
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split(FIELD_SEP)
                if (parts.size < 3) null
                else WidgetMeal(parts[0], parts[1], parts[2].toLongOrNull() ?: 0L)
            }

        return WidgetSnapshot(
            cravings = cravings,
            meals = meals,
            sessionCode = prefs.getString("sessionCode", null)?.takeIf { it.isNotBlank() },
            sessionName = prefs.getString("sessionName", null)?.takeIf { it.isNotBlank() }
        )
    }

    actual fun getPaneIndex(): Int = prefs.getInt("paneIndex", 0)

    actual fun savePaneIndex(index: Int) {
        prefs.edit().putInt("paneIndex", index).apply()
    }
}
