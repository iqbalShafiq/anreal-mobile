package co.ratmo.anreal.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

private val TokenKey = stringPreferencesKey("session_token")

class DataStoreSessionTokenStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: TokenCipher,
) : SessionTokenStore {

    override fun observe(): Flow<String?> {
        return dataStore.data.map { preferences -> decode(preferences[TokenKey]) }
    }

    override suspend fun token(): String? = observe().first()

    override suspend fun save(token: String) {
        dataStore.edit { preferences ->
            preferences[TokenKey] = cipher.encrypt(token)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(TokenKey) }
    }

    private fun decode(raw: String?): String? {
        if (raw == null) return null
        return runCatching { cipher.decrypt(raw) }.getOrNull()
    }
}

fun createSessionDataStore(producePath: () -> String): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() },
    )
}
