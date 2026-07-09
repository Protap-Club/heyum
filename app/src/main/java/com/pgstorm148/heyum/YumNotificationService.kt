package com.pgstorm148.heyum

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth as AndroidAuth
import com.google.firebase.firestore.FirebaseFirestore as AndroidFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class YumNotificationService : FirebaseMessagingService() {

    private val jobScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "YumNotificationService"
        
        // Channel IDs
        const val CHANNEL_REMINDERS = "meal_reminders"
        const val CHANNEL_REORDERS = "reorder_suggestions"
        const val CHANNEL_WASTE = "waste_alerts"
        const val CHANNEL_GROUPS = "group_invites"

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // 1. Meal Reminders
                val remindersChan = NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Meal Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Time-based prompts and contextual reminder updates"
                }

                // 2. Re-order Suggestions
                val reordersChan = NotificationChannel(
                    CHANNEL_REORDERS,
                    "Re-order Suggestions",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "One-tap reorders based on previous dining habits"
                }

                // 3. Fridge & Waste Alerts
                val wasteChan = NotificationChannel(
                    CHANNEL_WASTE,
                    "Fridge & Waste Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Urgent alerts for ingredients about to expire"
                }

                // 4. Group Invites
                val groupsChan = NotificationChannel(
                    CHANNEL_GROUPS,
                    "Group Invites",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Invites to decide food with friends"
                }

                manager.createNotificationChannel(remindersChan)
                manager.createNotificationChannel(reordersChan)
                manager.createNotificationChannel(wasteChan)
                manager.createNotificationChannel(groupsChan)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token generated: $token")
        // In real backend, upload token to the user's document in Firestore
        uploadTokenToBackend(token)
    }

    private fun uploadTokenToBackend(token: String) {
        val uid = AndroidAuth.getInstance().currentUser?.uid ?: return
        jobScope.launch {
            try {
                AndroidFirestore.getInstance().collection("users").document(uid)
                    .update("fcmToken", token).await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload FCM token: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        val data = remoteMessage.data
        val title = data["title"] ?: remoteMessage.notification?.title ?: "yum."
        val body = data["body"] ?: remoteMessage.notification?.body ?: "Time for something delicious."
        val type = data["type"] ?: "nudge" // nudge, reminder, reorder, waste, group
        val channelId = when (type) {
            "nudge", "reminder" -> CHANNEL_REMINDERS
            "reorder" -> CHANNEL_REORDERS
            "waste" -> CHANNEL_WASTE
            "group" -> CHANNEL_GROUPS
            else -> CHANNEL_REMINDERS
        }

        val dishId = data["dishId"]
        val groupId = data["groupId"]
        val ingredient = data["ingredient"]

        // Fetch User Settings from Firestore and check constraint filters
        val uid = AndroidAuth.getInstance().currentUser?.uid
        if (uid != null) {
            jobScope.launch {
                try {
                    val userDoc = AndroidFirestore.getInstance().collection("users").document(uid).get().await()
                    if (userDoc.exists()) {
                        // 1. Check Category Enable Toggles
                        val mealRemindersEnabled = userDoc.getBoolean("mealRemindersEnabled") ?: true
                        val reorderSuggestionsEnabled = userDoc.getBoolean("reorderSuggestionsEnabled") ?: true
                        val wasteAlertsEnabled = userDoc.getBoolean("wasteAlertsEnabled") ?: true
                        val groupInvitesEnabled = userDoc.getBoolean("groupInvitesEnabled") ?: true

                        val isEnabled = when (type) {
                            "nudge", "reminder" -> mealRemindersEnabled
                            "reorder" -> reorderSuggestionsEnabled
                            "waste" -> wasteAlertsEnabled
                            "group" -> groupInvitesEnabled
                            else -> true
                        }

                        if (!isEnabled) {
                            Log.d(TAG, "Suppressed notification of type $type: user toggle is off")
                            return@launch
                        }

                        // 2. Check Quiet Hours
                        val quietHoursEnabled = userDoc.getBoolean("quietHoursEnabled") ?: false
                        if (quietHoursEnabled) {
                            val startStr = userDoc.getString("quietHoursStart") ?: "23:00"
                            val endStr = userDoc.getString("quietHoursEnd") ?: "08:00"
                            if (isCurrentTimeInQuietHours(startStr, endStr)) {
                                Log.d(TAG, "Suppressed notification of type $type: quiet hours active ($startStr - $endStr)")
                                return@launch
                            }
                        }

                        // 3. Check Frequency Settings
                        val frequency = userDoc.getString("notificationFrequency") ?: "STANDARD"
                        if (frequency == "MINIMAL" && type == "reminder") {
                            // Filter out low priority reminders on Minimal
                            Log.d(TAG, "Suppressed reminder on minimal frequency setting")
                            return@launch
                        }
                    }
                    showNotification(title, body, channelId, type, dishId, groupId, ingredient)
                } catch (e: Exception) {
                    // Fallback to showing if database check fails
                    showNotification(title, body, channelId, type, dishId, groupId, ingredient)
                }
            }
        } else {
            showNotification(title, body, channelId, type, dishId, groupId, ingredient)
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        type: String,
        dishId: String?,
        groupId: String?,
        ingredient: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", type)
            dishId?.let { putExtra("dish_id", it) }
            groupId?.let { putExtra("group_id", it) }
            ingredient?.let { putExtra("ingredient", it) }
        }

        val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            piFlags
        )

        // Custom action for one-tap Reorder
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFFF6B35.toInt())
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(
                if (channelId == CHANNEL_WASTE || channelId == CHANNEL_GROUPS) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Add Quick Reorder Action if type is reorder
        if (type == "reorder" && dishId != null) {
            val reorderIntent = Intent(this, MainActivity::class.java).apply {
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("notification_type", "reorder_direct")
                putExtra("dish_id", dishId)
            }
            val reorderPendingIntent = PendingIntent.getActivity(
                this,
                999,
                reorderIntent,
                piFlags
            )
            builder.addAction(
                android.R.drawable.ic_menu_revert,
                "One-Tap Reorder 🍛",
                reorderPendingIntent
            )
        }

        try {
            val notificationId = (System.currentTimeMillis() % 100000).toInt()
            NotificationManagerCompat.from(this).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission POST_NOTIFICATIONS missing: ${e.message}")
        }
    }

    private fun isCurrentTimeInQuietHours(start: String, end: String): Boolean {
        try {
            val sdf = SimpleDateFormat("HH:mm", Locale.US)
            val nowStr = sdf.format(Calendar.getInstance().time)
            
            val timeStart = sdf.parse(start)!!
            val timeEnd = sdf.parse(end)!!
            val timeNow = sdf.parse(nowStr)!!

            return if (timeStart.before(timeEnd)) {
                // Same day quiet hours e.g. 09:00 to 17:00
                timeNow.after(timeStart) && timeNow.before(timeEnd)
            } else {
                // Overnight quiet hours e.g. 23:00 to 08:00
                timeNow.after(timeStart) || timeNow.before(timeEnd)
            }
        } catch (e: Exception) {
            return false
        }
    }
}
