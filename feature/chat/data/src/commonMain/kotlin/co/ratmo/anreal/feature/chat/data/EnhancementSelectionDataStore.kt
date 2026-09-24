package co.ratmo.anreal.feature.chat.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.core.scope.Scope

/**
 * Platform file-backed [DataStore] for enhancement (skills/MCP) selection,
 * mirroring how the session-token store is provided per platform.
 */
internal expect fun Scope.createEnhancementSelectionDataStore(): DataStore<Preferences>
