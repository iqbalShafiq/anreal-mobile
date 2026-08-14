package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.feature.chat.domain.ChatRepository
import org.koin.dsl.module

val chatDataModule = module {
    single { KtorChatRemoteDataSource(get()) }
    single { RoomChatLocalDataSource(get(), get()) }
    single<ChatRepository> { OfflineFirstChatRepository(get(), get()) }
}
