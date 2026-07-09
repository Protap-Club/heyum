package com.pgstorm148.heyum

import android.app.Application
import com.pgstorm148.heyum.di.initKoin
import com.pgstorm148.heyum.notifications.NotificationScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.pgstorm148.heyum.engine.MealLogRepository
import com.pgstorm148.heyum.engine.PreferenceRepository
import com.pgstorm148.heyum.db.AndroidMealLogRepository
import com.pgstorm148.heyum.db.AndroidPreferenceRepository

class YumApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@YumApplication)
            modules(module {
                single<MealLogRepository> { AndroidMealLogRepository(get()) }
                single<PreferenceRepository> { AndroidPreferenceRepository(get()) }
            })
        }
        YumNotificationService.createNotificationChannels(this)
        NotificationScheduler.scheduleMealNotifications(this)
    }
}
