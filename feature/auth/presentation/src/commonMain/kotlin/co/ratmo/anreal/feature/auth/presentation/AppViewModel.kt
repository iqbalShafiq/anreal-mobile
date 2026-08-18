package co.ratmo.anreal.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.ratmo.anreal.core.domain.model.User
import co.ratmo.anreal.core.domain.model.AppPreferences
import co.ratmo.anreal.core.domain.model.AppPreferencesRepository
import co.ratmo.anreal.feature.auth.domain.AuthSession
import co.ratmo.anreal.feature.auth.domain.SessionStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val authSession: AuthSession,
    preferencesRepository: AppPreferencesRepository,
) : ViewModel() {
    val preferences: StateFlow<AppPreferences> = preferencesRepository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppPreferences(),
    )
    val status: StateFlow<SessionStatus> = authSession.status.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SessionStatus.Checking,
    )
    val user: StateFlow<User?> = authSession.user.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    fun signOut() {
        viewModelScope.launch { authSession.signOut() }
    }
}
