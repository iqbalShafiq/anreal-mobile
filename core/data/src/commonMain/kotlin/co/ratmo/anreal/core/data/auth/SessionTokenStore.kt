package co.ratmo.anreal.core.data.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SessionTokenStore {
    fun observe(): Flow<String?>
    suspend fun token(): String?
    suspend fun save(token: String)
    suspend fun clear()
}

class InMemorySessionTokenStore : SessionTokenStore {
    private val value = MutableStateFlow<String?>(null)

    override fun observe(): Flow<String?> = value.asStateFlow()

    override suspend fun token(): String? = value.value

    override suspend fun save(token: String) {
        value.value = token
    }

    override suspend fun clear() {
        value.value = null
    }
}
