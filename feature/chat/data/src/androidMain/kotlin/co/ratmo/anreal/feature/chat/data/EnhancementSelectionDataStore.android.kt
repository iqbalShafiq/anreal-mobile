package co.ratmo.anreal.feature.chat.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import co.ratmo.anreal.core.data.auth.createSessionDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope

internal actual fun Scope.createEnhancementSelectionDataStore(): DataStore<Preferences> {
    val path = androidContext().applicationContext.filesDir
        .resolve("anreal_enhancement.preferences_pb")
        .absolutePath
    return createSessionDataStore { path }
}
