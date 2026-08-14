package co.ratmo.anreal.core.data.auth

interface SessionTokenStore {
    suspend fun token(): String?
    suspend fun save(token: String)
    suspend fun clear()
}

class InMemorySessionTokenStore : SessionTokenStore {
    private var value: String? = null

    override suspend fun token(): String? = value

    override suspend fun save(token: String) {
        value = token
    }

    override suspend fun clear() {
        value = null
    }
}
