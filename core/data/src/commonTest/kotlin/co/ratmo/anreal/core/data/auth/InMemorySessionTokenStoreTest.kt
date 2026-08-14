package co.ratmo.anreal.core.data.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class InMemorySessionTokenStoreTest {

    @Test
    fun save_read_and_clear_token() = runTest {
        val store = InMemorySessionTokenStore()

        assertThat(store.token()).isNull()

        store.save("abc.session")
        assertThat(store.token()).isEqualTo("abc.session")

        store.clear()
        assertThat(store.token()).isNull()
    }
}
