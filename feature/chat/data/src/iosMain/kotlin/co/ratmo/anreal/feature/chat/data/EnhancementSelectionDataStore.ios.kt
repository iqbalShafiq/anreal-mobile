package co.ratmo.anreal.feature.chat.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import co.ratmo.anreal.core.data.auth.createSessionDataStore
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.scope.Scope
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

internal actual fun Scope.createEnhancementSelectionDataStore(): DataStore<Preferences> {
    return createSessionDataStore { iosEnhancementDataStorePath() }
}

@OptIn(ExperimentalForeignApi::class)
private fun iosEnhancementDataStorePath(): String {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(directory?.path) + "/anreal_enhancement.preferences_pb"
}
