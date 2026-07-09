package com.pgstorm148.heyum.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val MEAL_NOTIFICATION_WORK_NAME = "meal_notification_work"

    fun scheduleMealNotifications(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Schedule periodic work to check and send meal notifications every hour
        val mealNotificationWork = PeriodicWorkRequestBuilder<MealNotificationWorker>(
            1,
            TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            MEAL_NOTIFICATION_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            mealNotificationWork
        )
    }

    fun cancelMealNotifications(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(MEAL_NOTIFICATION_WORK_NAME)
    }

    // Helper function to calculate initial delay to next meal time
    private fun calculateInitialDelay(): Long {
        val now = LocalDateTime.now()
        val mealTimes = listOf(
            LocalTime.of(9, 0),      // Breakfast
            LocalTime.of(11, 30),    // Brunch
            LocalTime.of(13, 30),    // Lunch
            LocalTime.of(16, 30),    // Evening snacks
            LocalTime.of(19, 30),    // Dinner
            LocalTime.of(1, 30)      // Late night snacks
        )

        var nextMealTime = mealTimes.find { it.isAfter(now.toLocalTime()) }?.atDate(now.toLocalDate())
            ?: mealTimes[0].atDate(now.toLocalDate().plusDays(1))

        // If next meal time is in the past, move to tomorrow
        if (nextMealTime.isBefore(now)) {
            nextMealTime = mealTimes[0].atDate(now.toLocalDate().plusDays(1))
        }

        val duration = java.time.Duration.between(now, nextMealTime)
        return duration.seconds
    }
}
