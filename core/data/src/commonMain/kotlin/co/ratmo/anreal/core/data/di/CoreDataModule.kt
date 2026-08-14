package co.ratmo.anreal.core.data.di

import co.ratmo.anreal.core.data.AppConfig
import co.ratmo.anreal.core.data.auth.sessionTokenModule
import co.ratmo.anreal.core.data.network.HttpClientFactory
import co.ratmo.anreal.core.data.network.platformHttpClientEngine
import org.koin.dsl.module

val coreDataModule = module {
    includes(sessionTokenModule)
    single {
        HttpClientFactory.create(
            engine = platformHttpClientEngine(),
            tokenStore = get(),
            baseUrl = get<AppConfig>().baseUrl,
        )
    }
}
