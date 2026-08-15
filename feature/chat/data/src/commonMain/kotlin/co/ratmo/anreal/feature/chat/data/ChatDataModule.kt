package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.data.AppConfig
import co.ratmo.anreal.feature.chat.domain.ChatRepository
import org.koin.dsl.module

val chatDataModule = module {
    single { KtorChatRemoteDataSource(get()) }
    single { RoomChatLocalDataSource(get(), get(), get()) }
    single<ChatRepository> {
        if (get<AppConfig>().environment.stubApi) {
            StubChatRepository()
        } else {
            OfflineFirstChatRepository(get(), get())
        }
    }
}
