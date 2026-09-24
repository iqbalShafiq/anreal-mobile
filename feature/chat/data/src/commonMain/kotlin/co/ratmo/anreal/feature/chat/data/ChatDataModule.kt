package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.data.AppConfig
import co.ratmo.anreal.feature.chat.domain.AccountSettingsDataSource
import co.ratmo.anreal.feature.chat.domain.ChatRepository
import co.ratmo.anreal.feature.chat.domain.SkillsRemoteDataSource
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
}
