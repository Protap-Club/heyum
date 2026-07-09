package com.pgstorm148.heyum.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.pgstorm148.heyum.R
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pgstorm148.heyum.MainActivity
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random

class MealNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                return Result.retry()
            }

            val mealType = determineMealType()
            val recommendation = getRecommendation(userId, mealType)

            sendNotification(mealType, recommendation)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun determineMealType(): MealType {
        val now = LocalTime.now()
        return when {
            now.isBefore(LocalTime.of(11, 30)) -> MealType.BREAKFAST
            now.isBefore(LocalTime.of(13, 30)) -> MealType.BRUNCH
            now.isBefore(LocalTime.of(16, 30)) -> MealType.LUNCH
            now.isBefore(LocalTime.of(19, 30)) -> MealType.EVENING_SNACKS
            now.isBefore(LocalTime.of(1, 30)) -> MealType.DINNER
            else -> MealType.LATE_NIGHT_SNACKS
        }
    }

    private suspend fun getRecommendation(userId: String, mealType: MealType): String {
        return try {
            val db = FirebaseFirestore.getInstance()

            // Get user's past cravings (40%)
            val cravingsSnapshot = db.collection("cravings")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()

            val pastCravings = cravingsSnapshot.documents
                .mapNotNull { it.getString("foodName") }
                .distinct()

            // Mix recommendations
            val recommendations = mutableListOf<String>()

            // 40% from past cravings
            if (pastCravings.isNotEmpty()) {
                repeat(2) {
                    recommendations.add(pastCravings.random())
                }
            }

            // 30% from popular meals
            recommendations.addAll(getPopularMeals(mealType).take(1))

            // 30% from favorites
            recommendations.addAll(getFavoriteMeals(mealType).take(1))

            recommendations.shuffled().firstOrNull() ?: getDefaultMeal(mealType)
        } catch (e: Exception) {
            getDefaultMeal(mealType)
        }
    }

    private fun getPopularMeals(mealType: MealType): List<String> {
        return when (mealType) {
            MealType.BREAKFAST -> listOf("Pancakes", "Oatmeal", "Eggs", "Toast", "Cereal")
            MealType.BRUNCH -> listOf("Avocado Toast", "Waffle", "Benedict Eggs", "French Toast")
            MealType.LUNCH -> listOf("Pasta", "Sandwich", "Salad", "Rice Bowl", "Burger")
            MealType.EVENING_SNACKS -> listOf("Samosa", "Cookies", "Cake", "Chips", "Fruit")
            MealType.DINNER -> listOf("Biryani", "Dal Makhani", "Roti", "Curry", "Steak")
            MealType.LATE_NIGHT_SNACKS -> listOf("Maggi", "Popcorn", "Nuts", "Yogurt", "Ice Cream")
        }
    }

    private fun getFavoriteMeals(mealType: MealType): List<String> {
        return when (mealType) {
            MealType.BREAKFAST -> listOf("Idli", "Dosa", "Paratha")
            MealType.BRUNCH -> listOf("Chilla", "Upma", "Dhokla")
            MealType.LUNCH -> listOf("Biryani", "Butter Chicken", "Tandoori")
            MealType.EVENING_SNACKS -> listOf("Chai & Pakora", "Tea & Biscuits")
            MealType.DINNER -> listOf("Paneer Tikka", "Naan", "Raita")
            MealType.LATE_NIGHT_SNACKS -> listOf("Chai", "Cha Paratha")
        }
    }

    private fun getDefaultMeal(mealType: MealType): String {
        // Always fall back to a real dish, never the meal-category label. Otherwise the copy reads
        // "What's for Late Night Snacks? We suggest: Late Night Snacks", which is what users saw.
        return getPopularMeals(mealType).random()
    }

    private fun sendNotification(mealType: MealType, recommendation: String) {
        // Guard against the recommendation collapsing to the category name (e.g. an old cached
        // value or a blank), which is what produced the repetitive notification text.
        val label = mealLabel(mealType)
        val dish = recommendation.trim()
            .takeIf { it.isNotEmpty() && !it.equals(label, ignoreCase = true) }
            ?: getDefaultMeal(mealType)

        val title = buildTitle(mealType)
        val message = buildMessage(mealType, dish)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, "meal_reminders")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFFF6B35.toInt())
            .setLargeIcon(BitmapFactory.decodeResource(applicationContext.resources, R.mipmap.ic_launcher))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Random.nextInt(), notification)
    }

    /** Human, lowercase label for use inside a sentence, e.g. "late-night snacks". */
    private fun mealLabel(mealType: MealType): String {
        return when (mealType) {
            MealType.BREAKFAST -> "breakfast"
            MealType.BRUNCH -> "brunch"
            MealType.LUNCH -> "lunch"
            MealType.EVENING_SNACKS -> "evening snacks"
            MealType.DINNER -> "dinner"
            MealType.LATE_NIGHT_SNACKS -> "late-night snacks"
        }
    }

    /** A catchy, non-repetitive title that sets the mood rather than echoing the body. */
    private fun buildTitle(mealType: MealType): String {
        val options = when (mealType) {
            MealType.BREAKFAST -> listOf("☀️ Rise & dine", "Good morning, hungry?")
            MealType.BRUNCH -> listOf("🥂 Brunch o'clock", "Lazy brunch vibes?")
            MealType.LUNCH -> listOf("🍴 Lunch, decided", "Midday fuel time")
            MealType.EVENING_SNACKS -> listOf("🫖 Snack o'clock", "Chai & something?")
            MealType.DINNER -> listOf("🌆 Dinner, sorted", "What's for dinner?")
            MealType.LATE_NIGHT_SNACKS -> listOf("🌙 Late-night cravings", "Midnight munchies?")
        }
        return options.random()
    }

    /** Varied body copy that always names the actual dish, never the meal category. */
    private fun buildMessage(mealType: MealType, dish: String): String {
        val label = mealLabel(mealType)
        val templates = listOf(
            "Craving $dish? Tap to lock it in 😋",
            "$dish for $label? One swipe and you're sorted.",
            "How about $dish right now? 🍽️",
            "$dish is calling your name — yum or nah?",
            "Can't decide on $label? We're feeling $dish today."
        )
        return templates.random()
    }
}

enum class MealType {
    BREAKFAST,
    BRUNCH,
    LUNCH,
    EVENING_SNACKS,
    DINNER,
    LATE_NIGHT_SNACKS
}
