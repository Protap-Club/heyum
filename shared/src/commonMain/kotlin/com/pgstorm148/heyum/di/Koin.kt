package com.pgstorm148.heyum.di

import com.pgstorm148.heyum.api.FridgeScannerAPI
import com.pgstorm148.heyum.api.SuggestionEngineClient
import com.pgstorm148.heyum.ui.screens.DecisionCardScreenModel
import com.pgstorm148.heyum.engine.WidgetCache
import com.pgstorm148.heyum.engine.WidgetSuggestionEngine
import com.pgstorm148.heyum.engine.KitchenStorage
import com.pgstorm148.heyum.firebase.AuthRepository
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

expect val platformModule: Module

val sharedModule = module {
    single { AuthRepository(get()) }
    single { SuggestionEngineClient(get(), get()) }
    single { FridgeScannerAPI() }
    single { WidgetCache() }
    single { KitchenStorage() }
    single { WidgetSuggestionEngine(get(), get(), get()) }
    factory { DecisionCardScreenModel(get(), get(), get(), get(), get(), get(), get(), get()) }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(platformModule, sharedModule)
}

fun initKoin() = initKoin {}
