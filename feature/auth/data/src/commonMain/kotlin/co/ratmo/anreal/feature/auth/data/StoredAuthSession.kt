package co.ratmo.anreal.feature.auth.data

import co.ratmo.anreal.core.data.auth.SessionTokenStore
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.auth.domain.AuthRemoteDataSource
import co.ratmo.anreal.feature.auth.domain.AuthSession
import co.ratmo.anreal.feature.auth.domain.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class StoredAuthSession(
    private val tokenStore: SessionTokenStore,
    private val remote: AuthRemoteDataSource,
) : AuthSession {

    override val status: Flow<SessionStatus> = flow {
        emit(SessionStatus.Checking)
        emit(resolve())
        emitAll(
            tokenStore.observe().drop(1).map { token ->
                if (token == null) SessionStatus.SignedOut else resolve()
            },
        )
    }.distinctUntilChanged()

    private suspend fun resolve(): SessionStatus {
        if (tokenStore.token() == null) return SessionStatus.SignedOut
        return when (val result = remote.currentUser()) {
            is Result.Success -> if (result.data != null) {
                SessionStatus.SignedIn
            } else {
                tokenStore.clear()
                SessionStatus.SignedOut
            }
            is Result.Error -> SessionStatus.SignedIn
        }
    }
}
