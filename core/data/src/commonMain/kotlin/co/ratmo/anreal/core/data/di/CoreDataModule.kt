package co.ratmo.anreal.core.data.di

import co.ratmo.anreal.core.data.AppConfig
import co.ratmo.anreal.core.data.auth.sessionTokenModule
import co.ratmo.anreal.core.data.network.HttpClientFactory
import co.ratmo.anreal.core.data.network.platformHttpClientEngine
import co.ratmo.anreal.core.data.preferences.appPreferencesModule
import org.koin.dsl.module

val coreDataModule = module {
    includes(sessionTokenModule, appPreferencesModule)
    single {
        val config = get<AppConfig>()
        HttpClientFactory.create(
            engine = platformHttpClientEngine(),
            tokenStore = get(),
            baseUrl = config.baseUrl,
            enableNetworkLogging = config.isDebug,
        )
    }
}
