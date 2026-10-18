package com.pgstorm148.heyum

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pgstorm148.heyum.engine.WidgetDataStore
import com.pgstorm148.heyum.engine.WidgetPane
import com.pgstorm148.heyum.engine.WidgetSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

// Warm palette, matching the in-app theme.
private val BackgroundColor = ColorProvider(Color(0xFFFFF8F0))
private val WidgetTextColor = ColorProvider(Color(0xFF1A1A1A))
private val MutedTextColor = ColorProvider(Color(0xFF75706B))
private val AccentColor = ColorProvider(Color(0xFFFF6B35))

/**
 * Home-screen widget showing the user's craving log, the meals they've logged, and any live group
 * session.
 *
 * Sizing: small shows one pane at a time, medium shows cravings + meals side by side, and large
 * shows all three including a tap-to-join session row.
 *
 * On the small size the panes cycle via the "›" button rather than a swipe: Glance renders through
 * RemoteViews, which supports click actions but has no swipe/gesture support, so a tap target is
 * the closest the platform allows.
 */
class YumSuggestionWidget : GlanceAppWidget() {

    // Without this, Glance defaults to SizeMode.Single and LocalSize always reports the *minimum*
    // size declared in yum_widget_info.xml (110x110dp) regardless of how large the user actually
    // made the widget — which made the medium and large layouts unreachable.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val size = LocalSize.current
            val ctx = LocalContext.current
            val snapshot = loadSnapshot()

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(BackgroundColor)
                    .padding(12.dp)
            ) {
                // One vertical pane at every size, with arrows to rotate between them.
                // Showing all three stacked (the old large layout) left big empty gaps on a
                // wide widget; a single pane fills the space and reads the same everywhere.
                // Taller widgets simply show more rows.
                val rows = when {
                    size.height >= 220.dp -> 8
                    size.height >= 140.dp -> 4
                    else -> 3
                }
                PaneWidget(ctx, snapshot, currentPane(), rows)
            }
        }
    }

    /**
     * A single pane filling the widget vertically, with prev/next arrows in the header.
     *
     * Glance renders through RemoteViews, which has no swipe/gesture support, so the arrows are
     * the closest the platform allows to sliding between panes.
     */
    @Composable
    private fun PaneWidget(context: Context, snapshot: WidgetSnapshot, pane: WidgetPane, rows: Int) {
        Column(
            modifier = GlanceModifier.fillMaxSize().clickable(openAppAction(context, pane, snapshot)),
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = paneTitle(pane),
                    style = TextStyle(color = AccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.defaultWeight()
                )
                // Generous padding so these stay tappable at widget scale.
                Text(
                    text = " ‹ ",
                    style = TextStyle(color = MutedTextColor, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.clickable(actionRunCallback<PrevPaneCallback>()).padding(4.dp)
                )
                Text(
                    text = " › ",
                    style = TextStyle(color = MutedTextColor, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.clickable(actionRunCallback<CyclePaneCallback>()).padding(4.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            when (pane) {
                WidgetPane.CRAVINGS -> CravingList(snapshot, maxLines = rows)
                WidgetPane.MEALS -> MealList(snapshot, maxLines = rows)
                WidgetPane.SESSION -> SessionPane(snapshot, compact = true)
            }
        }
    }

    @Composable
    private fun CravingList(snapshot: WidgetSnapshot, maxLines: Int) {
        if (snapshot.cravings.isEmpty()) {
            EmptyHint("No cravings yet — tap to log one")
            return
        }
        Column {
            snapshot.cravings.take(maxLines).forEach { craving ->
                Text(
                    text = "• ${craving.foodName}",
                    style = TextStyle(color = WidgetTextColor, fontSize = 12.sp),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
            }
        }
    }

    @Composable
    private fun MealList(snapshot: WidgetSnapshot, maxLines: Int) {
        if (snapshot.meals.isEmpty()) {
            EmptyHint("Nothing logged today")
            return
        }
        Column {
            snapshot.meals.take(maxLines).forEach { meal ->
                Text(
                    text = "• ${meal.dishName}",
                    style = TextStyle(color = WidgetTextColor, fontSize = 12.sp),
                    maxLines = 1
                )
                if (meal.mealType.isNotBlank() && meal.mealType != "ALL") {
                    Text(
                        text = meal.mealType.lowercase(),
                        style = TextStyle(color = MutedTextColor, fontSize = 9.sp)
                    )
                }
                Spacer(modifier = GlanceModifier.height(2.dp))
            }
        }
    }

    @Composable
    private fun SessionPane(snapshot: WidgetSnapshot, compact: Boolean) {
        val context = LocalContext.current
        val code = snapshot.sessionCode
        if (code.isNullOrBlank()) {
            Column {
                if (!compact) {
                    Text(
                        text = paneTitle(WidgetPane.SESSION),
                        style = TextStyle(color = AccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }
                EmptyHint("No group session running")
            }
            return
        }

        // A live session: the whole block jumps straight into that session in the app.
        Column(
            modifier = GlanceModifier.fillMaxWidth().clickable(joinSessionAction(context, code))
        ) {
            Text(
                text = paneTitle(WidgetPane.SESSION),
                style = TextStyle(color = AccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = snapshot.sessionName?.takeIf { it.isNotBlank() } ?: "Group session",
                style = TextStyle(color = WidgetTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "Tap to join · $code",
                style = TextStyle(color = MutedTextColor, fontSize = 10.sp),
                maxLines = 1
            )
        }
    }

    @Composable
    private fun EmptyHint(message: String) {
        Text(
            text = message,
            style = TextStyle(color = MutedTextColor, fontSize = 11.sp, textAlign = TextAlign.Start),
            maxLines = 2
        )
    }

    private fun paneTitle(pane: WidgetPane): String = when (pane) {
        WidgetPane.CRAVINGS -> "CRAVING LOG"
        WidgetPane.MEALS -> "MEALS LOGGED"
        WidgetPane.SESSION -> "GROUP SESSION"
    }

    private fun loadSnapshot(): WidgetSnapshot = try {
        GlobalContext.get().get<WidgetDataStore>().load()
    } catch (e: Exception) {
        WidgetSnapshot()
    }

    private fun currentPane(): WidgetPane = try {
        val index = GlobalContext.get().get<WidgetDataStore>().getPaneIndex()
        WidgetPane.entries[index.mod(WidgetPane.entries.size)]
    } catch (e: Exception) {
        WidgetPane.CRAVINGS
    }

    /** Opens the app on the screen matching the tapped pane. */
    private fun openAppAction(context: Context, pane: WidgetPane, snapshot: WidgetSnapshot): Action {
        val code = snapshot.sessionCode
        if (pane == WidgetPane.SESSION) {
            // A live session jumps straight into it; with no session, open the app's group
            // sheet so the tap still does something instead of silently going nowhere.
            return if (!code.isNullOrBlank()) joinSessionAction(context, code)
            else payloadAction(context, "group_new")
        }
        return payloadAction(context, if (pane == WidgetPane.MEALS) "meal_log" else "craving_log")
    }

    private fun payloadAction(context: Context, type: String): Action {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", type)
        }
        return actionStartActivity(intent)
    }

    /** Jumps directly into the live group session, reusing MainActivity's group payload route. */
    private fun joinSessionAction(context: Context, joinCode: String): Action {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "group")
            putExtra("group_id", joinCode)
        }
        return actionStartActivity(intent)
    }
}

/** Steps the widget back to the previous pane and re-renders in place. */
class PrevPaneCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters
    ) {
        try {
            val store = GlobalContext.get().get<WidgetDataStore>()
            store.savePaneIndex((store.getPaneIndex() - 1).mod(WidgetPane.entries.size))
            YumSuggestionWidget().updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/** Advances the widget to the next pane and re-renders in place. */
class CyclePaneCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters
    ) {
        try {
            val store = GlobalContext.get().get<WidgetDataStore>()
            store.savePaneIndex((store.getPaneIndex() + 1).mod(WidgetPane.entries.size))
            YumSuggestionWidget().updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class YumSuggestionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = YumSuggestionWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.pgstorm148.heyum.action.UPDATE_WIDGET") {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    YumSuggestionWidget().updateAll(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
