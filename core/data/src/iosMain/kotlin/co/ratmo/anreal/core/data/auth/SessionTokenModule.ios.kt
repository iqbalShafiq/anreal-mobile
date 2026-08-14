package co.ratmo.anreal.core.data.auth

import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual val sessionTokenModule: Module = module {
    // TODO: wrap the value with the iOS Keychain instead of a pass-through cipher.
    single<TokenCipher> { PassThroughTokenCipher() }
    single<SessionTokenStore> {
        val path = iosSessionDataStorePath()
        DataStoreSessionTokenStore(
            dataStore = createSessionDataStore { path },
            cipher = get(),
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun iosSessionDataStorePath(): String {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(directory?.path) + "/anreal_session.preferences_pb"
}
