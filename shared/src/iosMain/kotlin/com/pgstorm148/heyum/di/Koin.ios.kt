package com.pgstorm148.heyum.di

import com.pgstorm148.heyum.firebase.*
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { FirebaseAuth() }
    single { FirebaseFirestore() }
    single<DishRepository> { IosDishRepository() }
    single<UserRepository> { IosUserRepository() }
    single<GroupSessionRepository> { IosGroupSessionRepository() }
    single<AmbienceRepository> { IosAmbienceRepository() }
    single<com.pgstorm148.heyum.engine.MealLogRepository> { com.pgstorm148.heyum.engine.IosMealLogRepository() }
    single<com.pgstorm148.heyum.engine.PreferenceRepository> { com.pgstorm148.heyum.engine.IosPreferenceRepository() }
}
