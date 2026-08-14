package co.ratmo.anreal.feature.auth.presentation

import co.ratmo.anreal.core.domain.model.User
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.auth.domain.AuthError
import co.ratmo.anreal.feature.auth.domain.AuthRemoteDataSource

class FakeAuthRemoteDataSource : AuthRemoteDataSource {
    var signInResult: Result<User, AuthError> = Result.Success(
        User(id = "1", email = "a@b.com", name = "Ada"),
    )
    var signUpResult: Result<User, AuthError> = signInResult
    var signedInEmail: String? = null

    override suspend fun signIn(email: String, password: String): Result<User, AuthError> {
        signedInEmail = email
        return signInResult
    }

    override suspend fun signUp(
        name: String,
        email: String,
        password: String,
    ): Result<User, AuthError> {
        signedInEmail = email
        return signUpResult
    }

    override suspend fun signOut(): EmptyResult<AuthError> = Result.Success(Unit)

    override suspend fun currentUser(): Result<User?, AuthError> = Result.Success(null)
}
