package co.ratmo.anreal.feature.auth.domain

import co.ratmo.anreal.core.domain.model.User
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result

interface AuthRemoteDataSource {
    suspend fun signIn(email: String, password: String): Result<User, AuthError>
    suspend fun signUp(name: String, email: String, password: String): Result<User, AuthError>
    suspend fun signOut(): EmptyResult<AuthError>
    suspend fun currentUser(): Result<User?, AuthError>
}
