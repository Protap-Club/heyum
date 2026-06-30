package com.pgstorm148.heyum

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import com.pgstorm148.heyum.engine.WidgetCache
import com.pgstorm148.heyum.engine.WidgetSuggestionEngine
import com.pgstorm148.heyum.model.Cuisine
import com.pgstorm148.heyum.model.Dish
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class YumSuggestionWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val size = LocalSize.current
            val context = LocalContext.current
            
            val dish = getCachedDish()
            val openAppAction = getOpenAppAction(context, dish)

            // Warm Palette Colors Provider
            val backgroundColor = ColorProvider(android.graphics.Color.parseColor("#FFF8F0")) // Cream
            val textColor = ColorProvider(android.graphics.Color.parseColor("#1A1A1A")) // Charcoal
            val mutedTextColor = ColorProvider(android.graphics.Color.parseColor("#75706B")) // Slate
            val accentColor = ColorProvider(android.graphics.Color.parseColor("#FF6B35")) // Orange

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(12.dp)
                    .clickable(openAppAction)
            ) {
                if (size.width >= 170.dp) {
                    // Medium Widget (4x2)
                    Row(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Section: Suggestions detail
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val foodEmoji = when (dish.cuisine) {
                                    Cuisine.INDIAN -> "🍛"
                                    Cuisine.CHINESE -> "🥢"
                                    Cuisine.MEXICAN -> "🌮"
                                    Cuisine.HEALTHY -> "🥗"
                                    else -> "🍲"
                                }
                                Text(
                                    text = "$foodEmoji ",
                                    style = TextStyle(fontSize = 24.sp)
                                )
                                Text(
                                    text = dish.cuisine.name.lowercase(),
                                    style = TextStyle(
                                        color = accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            
                            Text(
                                text = dish.name,
                                style = TextStyle(
                                    color = textColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            
                            Text(
                                text = dish.description,
                                style = TextStyle(
                                    color = mutedTextColor,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(8.dp))

                        // Right Section: Interactive Decoders Actions
                        Column(
                            modifier = GlanceModifier.width(100.dp).fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Yum! button (opens post-decision in app)
                            Button(
                                text = "Yum! 👍",
                                onClick = openAppAction,
                                modifier = GlanceModifier.fillMaxWidth().height(36.dp)
                            )
                            
                            Spacer(modifier = GlanceModifier.height(6.dp))

                            // Nah button (cycles in background without app opening)
                            Button(
                                text = "Nah 👎",
                                onClick = actionRunCallback<NahActionCallback>(),
                                modifier = GlanceModifier.fillMaxWidth().height(36.dp)
                            )
                        }
                    }
                } else {
                    // Small Widget (2x2)
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val foodEmoji = when (dish.cuisine) {
                            Cuisine.INDIAN -> "🍛"
                            Cuisine.CHINESE -> "🥢"
                            Cuisine.MEXICAN -> "🌮"
                            Cuisine.HEALTHY -> "🥗"
                            else -> "🍲"
                        }
                        
                        Text(
                            text = foodEmoji,
                            style = TextStyle(fontSize = 32.sp)
                        )
                        
                        Spacer(modifier = GlanceModifier.height(6.dp))
                        
                        Text(
                            text = dish.name,
                            style = TextStyle(
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 1
                        )
                        
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        
                        Text(
                            text = "tap to decide",
                            style = TextStyle(
                                color = mutedTextColor,
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        }
    }

    private fun getCachedDish(): Dish {
        return try {
            val cache = GlobalContext.get().get<WidgetCache>()
            cache.getCachedDish() ?: getDefaultFallbackDish()
        } catch (e: Exception) {
            getDefaultFallbackDish()
        }
    }

    private fun getOpenAppAction(context: Context, dish: Dish): Action {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "reorder_direct")
            putExtra("dish_id", dish.id)
        }
        return actionStartActivity(intent)
    }

    private fun getDefaultFallbackDish() = Dish(
        id = "ginger_tea",
        name = "Ginger Cardamom Chai",
        cuisine = Cuisine.COMFORT,
        description = "Warm tea with cardamom and ginger.",
        ingredients = listOf("Tea leaves", "Milk", "Ginger")
    )
}

// ----------------------------------------------------
// Native Glance Action Callback for background Nah click
// ----------------------------------------------------
class NahActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: androidx.glance.action.ActionParameters) {
        try {
            val engine = GlobalContext.get().get<WidgetSuggestionEngine>()
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid ?: "default_user_123"

            // Perform background cycle (Nah)
            engine.recordUserAction(userId, accepted = false)
            engine.refreshSuggestion(userId)

            // Re-render
            YumSuggestionWidget().updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// ----------------------------------------------------
// AppWidget Provider Receiver
// ----------------------------------------------------
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

    override fun onUpdate(context: Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        
        // Refresh suggestion database in background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val engine = GlobalContext.get().get<WidgetSuggestionEngine>()
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val userId = auth.currentUser?.uid ?: "default_user_123"
                
                engine.refreshSuggestion(userId)
                
                YumSuggestionWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
