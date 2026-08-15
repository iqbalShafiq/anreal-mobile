package co.ratmo.anreal.feature.auth.data

import co.ratmo.anreal.core.data.auth.SessionTokenStore
import co.ratmo.anreal.core.domain.model.User
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.auth.domain.AuthError
import co.ratmo.anreal.feature.auth.domain.AuthRemoteDataSource

class StubAuthRemoteDataSource(
    private val tokenStore: SessionTokenStore,
) : AuthRemoteDataSource {

    override suspend fun signIn(email: String, password: String): Result<User, AuthError> {
        return succeed(email = email.trim())
    }

    override suspend fun signUp(
        name: String,
        email: String,
        password: String,
    ): Result<User, AuthError> {
        return succeed(email = email.trim(), name = name.trim().ifBlank { STUB_NAME })
    }

    override suspend fun signOut(): EmptyResult<AuthError> {
        tokenStore.clear()
        return Result.Success(Unit)
    }

    override suspend fun currentUser(): Result<User?, AuthError> {
        if (tokenStore.token() == null) return Result.Success(null)
        return Result.Success(stubUser())
    }

    private suspend fun succeed(
        email: String,
        name: String = email.substringBefore("@").ifBlank { STUB_NAME },
    ): Result<User, AuthError> {
        val user = User(
            id = STUB_USER_ID,
            email = email.ifBlank { STUB_EMAIL },
            name = name,
        )
        tokenStore.save(STUB_TOKEN)
        return Result.Success(user)
    }

    private fun stubUser(): User = User(id = STUB_USER_ID, email = STUB_EMAIL, name = STUB_NAME)

    private companion object {
        const val STUB_TOKEN = "dev-session-token"
        const val STUB_USER_ID = "dev-user"
        const val STUB_EMAIL = "dev@anreal.local"
        const val STUB_NAME = "Developer"
    }
}
