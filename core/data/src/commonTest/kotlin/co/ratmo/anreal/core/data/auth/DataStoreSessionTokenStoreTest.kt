package co.ratmo.anreal.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DataStoreSessionTokenStoreTest {

    @Test
    fun save_read_and_clear_token() = runTest {
        val store = store()

        assertThat(store.token()).isNull()

        store.save("abc.session")
        assertThat(store.token()).isEqualTo("abc.session")

        store.clear()
        assertThat(store.token()).isNull()
    }

    @Test
    fun observe_emits_saved_and_cleared_values() = runTest {
        val store = store()
        store.observe().test {
            assertThat(awaitItem()).isNull()
            store.save("tok")
            assertThat(awaitItem()).isEqualTo("tok")
            store.clear()
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun persisted_value_is_ciphertext_not_plaintext() = runTest {
        val cipher = ReversingTokenCipher()
        val store = store(cipher)
        store.save("secret-token")
        assertThat(store.token()).isEqualTo("secret-token")
        assertThat(cipher.lastWritten).isEqualTo("nekot-terces")
    }

    private fun store(cipher: TokenCipher = PassThroughTokenCipher()): DataStoreSessionTokenStore {
        return DataStoreSessionTokenStore(
            dataStore = MemoryPreferencesStore(),
            cipher = cipher,
        )
    }
}

private class MemoryPreferencesStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = state
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val next = transform(state.value)
        state.value = next
        return next
    }
}

private class ReversingTokenCipher : TokenCipher {
    var lastWritten: String? = null

    override fun encrypt(plaintext: String): String {
        return plaintext.reversed().also { lastWritten = it }
    }

    override fun decrypt(ciphertext: String): String = ciphertext.reversed()
}
