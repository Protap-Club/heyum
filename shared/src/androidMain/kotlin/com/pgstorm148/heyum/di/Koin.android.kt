package com.pgstorm148.heyum.di

import com.pgstorm148.heyum.firebase.*
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { FirebaseAuth() }
    single { FirebaseFirestore() }
    single<DishRepository> { AndroidDishRepository() }
    single<UserRepository> { AndroidUserRepository() }
    single<GroupSessionRepository> { AndroidGroupSessionRepository() }
    single<AmbienceRepository> { AndroidAmbienceRepository() }
}
