package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.data.AppConfig
import co.ratmo.anreal.feature.chat.domain.AccountSettingsDataSource
import co.ratmo.anreal.feature.chat.domain.ChatRepository
import co.ratmo.anreal.feature.chat.domain.EnhancementSelectionStore
import co.ratmo.anreal.feature.chat.domain.McpRemoteDataSource
import co.ratmo.anreal.feature.chat.domain.SiteBaseUrlProvider
import co.ratmo.anreal.feature.chat.domain.SkillsRemoteDataSource
import co.ratmo.anreal.feature.chat.domain.SitesRemoteDataSource
import org.koin.dsl.module

val chatDataModule = module {
    single { KtorChatRemoteDataSource(get()) }
    single { RoomChatLocalDataSource(get(), get(), get()) }
    single { RoomModelCatalogLocalDataSource(get()) }
    single<ChatRepository> {
        if (get<AppConfig>().environment.stubApi) {
            StubChatRepository()
        } else {
            OfflineFirstChatRepository(get(), get(), get())
        }
    }
    single<AccountSettingsDataSource> {
        if (get<AppConfig>().environment.stubApi) {
            StubAccountSettingsDataSource()
        } else {
            KtorAccountSettingsDataSource(get())
        }
    }
    single<SkillsRemoteDataSource> {
        if (get<AppConfig>().environment.stubApi) {
            StubSkillsDataSource()
        } else {
            KtorSkillsDataSource(get())
        }
    }
    single<McpRemoteDataSource> {
        if (get<AppConfig>().environment.stubApi) {
            StubMcpDataSource()
        } else {
            KtorMcpDataSource(get())
        }
    }
    single<SitesRemoteDataSource> {
        if (get<AppConfig>().environment.stubApi) {
            StubSitesDataSource()
        } else {
            KtorSitesDataSource(get())
        }
    }
    single<SiteBaseUrlProvider> { AppConfigSiteBaseUrlProvider(get()) }
    single<EnhancementSelectionStore> {
        if (get<AppConfig>().environment.stubApi) {
            InMemoryEnhancementSelectionStore()
        } else {
            DataStoreEnhancementSelectionStore(createEnhancementSelectionDataStore())
        }
    }
}

private class AppConfigSiteBaseUrlProvider(
    private val appConfig: AppConfig,
) : SiteBaseUrlProvider {
    override fun baseUrl(): String = appConfig.baseUrl
}
