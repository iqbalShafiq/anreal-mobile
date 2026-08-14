package co.ratmo.anreal.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.ratmo.anreal.feature.auth.domain.AuthSession
import co.ratmo.anreal.feature.auth.domain.SessionStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AppViewModel(
    authSession: AuthSession,
) : ViewModel() {
    val status: StateFlow<SessionStatus> = authSession.status.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SessionStatus.Checking,
    )
}
