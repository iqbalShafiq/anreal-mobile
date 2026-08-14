package co.ratmo.anreal.feature.auth.data

import co.ratmo.anreal.core.data.auth.SessionTokenStore
import co.ratmo.anreal.core.data.network.get
import co.ratmo.anreal.core.data.network.post
import co.ratmo.anreal.core.domain.model.User
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.domain.util.asEmptyResult
import co.ratmo.anreal.core.domain.util.mapError
import co.ratmo.anreal.core.domain.util.onSuccess
import co.ratmo.anreal.feature.auth.domain.AuthError
import co.ratmo.anreal.feature.auth.domain.AuthRemoteDataSource
import io.ktor.client.HttpClient

class KtorAuthDataSource(
    private val httpClient: HttpClient,
    private val tokenStore: SessionTokenStore,
) : AuthRemoteDataSource {

    override suspend fun signIn(email: String, password: String): Result<User, AuthError> {
        return httpClient.post<AuthCredentialsDto, AuthSessionResponseDto>(
            route = "/api/auth/sign-in/email",
            body = AuthCredentialsDto(email = email, password = password),
        ).toAuthUserResult()
    }

    override suspend fun signUp(
        name: String,
        email: String,
        password: String,
    ): Result<User, AuthError> {
        return httpClient.post<AuthCredentialsDto, AuthSessionResponseDto>(
            route = "/api/auth/sign-up/email",
            body = AuthCredentialsDto(email = email, password = password, name = name),
        ).toAuthUserResult(emailTakenOnConflict = true)
    }

    override suspend fun signOut(): EmptyResult<AuthError> {
        return httpClient.post(route = "/api/auth/sign-out")
            .toAuthResult()
            .onSuccess { tokenStore.clear() }
            .asEmptyResult()
    }

    override suspend fun currentUser(): Result<User?, AuthError> {
        return when (val result = httpClient.get<AuthSessionResponseDto>(route = "/api/auth/get-session")) {
            is Result.Success -> Result.Success(result.data.user?.toUser())
            is Result.Error -> if (result.error == DataError.Network.UNAUTHORIZED) {
                Result.Success(null)
            } else {
                Result.Error(AuthError.Network(result.error))
            }
        }
    }
}

private fun Result<AuthSessionResponseDto, DataError.Network>.toAuthUserResult(
    emailTakenOnConflict: Boolean = false,
): Result<User, AuthError> {
    return when (val mapped = mapError { it.toAuthError(emailTakenOnConflict) }) {
        is Result.Success -> {
            val user = mapped.data.user?.toUser()
            if (user == null) Result.Error(AuthError.Network(DataError.Network.SERIALIZATION))
            else Result.Success(user)
        }
        is Result.Error -> mapped
    }
}

private fun Result<Unit, DataError.Network>.toAuthResult(): Result<Unit, AuthError> {
    return mapError { it.toAuthError() }
}

private fun DataError.Network.toAuthError(emailTakenOnConflict: Boolean = false): AuthError {
    return when (this) {
        DataError.Network.UNAUTHORIZED -> AuthError.InvalidCredentials
        DataError.Network.CONFLICT -> if (emailTakenOnConflict) {
            AuthError.EmailTaken
        } else {
            AuthError.Network(this)
        }
        else -> AuthError.Network(this)
    }
}
