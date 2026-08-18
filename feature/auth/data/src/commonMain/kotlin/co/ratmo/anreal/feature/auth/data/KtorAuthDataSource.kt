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
        ).toAuthUserResult(detectEmailTaken = true)
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
            is Result.Error -> if (result.error.kind == DataError.Network.Kind.UNAUTHORIZED) {
                Result.Success(null)
            } else {
                Result.Error(AuthError.Network(result.error))
            }
        }
    }
}

private fun Result<AuthSessionResponseDto, DataError.Network>.toAuthUserResult(
    detectEmailTaken: Boolean = false,
): Result<User, AuthError> {
    return when (val mapped = mapError { it.toAuthError(detectEmailTaken) }) {
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

private fun DataError.Network.toAuthError(detectEmailTaken: Boolean = false): AuthError {
    return when {
        kind == DataError.Network.Kind.UNAUTHORIZED -> AuthError.InvalidCredentials
        detectEmailTaken && isEmailTaken() -> AuthError.EmailTaken
        else -> AuthError.Network(this)
    }
}

private fun DataError.Network.isEmailTaken(): Boolean {
    val normalizedCode = code.orEmpty().uppercase()
    val normalizedMessage = serverMessage.orEmpty().lowercase()
    return normalizedCode.contains("USER_ALREADY_EXISTS") ||
        normalizedCode.contains("EMAIL_ALREADY") ||
        normalizedMessage.contains("already exists") ||
        normalizedMessage.contains("email is already") ||
        normalizedMessage.contains("email already")
}
