package co.ratmo.anreal.feature.auth.domain

import co.ratmo.anreal.core.domain.model.User
import kotlinx.coroutines.flow.Flow

sealed interface SessionStatus {
    data object Checking : SessionStatus
    data object SignedOut : SessionStatus
    data object SignedIn : SessionStatus
}

interface AuthSession {
    val status: Flow<SessionStatus>
    val user: Flow<User?>
    suspend fun signOut()
}
