package co.ratmo.anreal.feature.chat.presentation.account

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.domain.model.AppPreferencesRepository
import co.ratmo.anreal.core.domain.model.AppThemeMode
import co.ratmo.anreal.feature.chat.domain.AccountSettingsDataSource
import co.ratmo.anreal.feature.chat.domain.PersonalizationProfile
import co.ratmo.anreal.feature.chat.domain.UsageSummary
import co.ratmo.anreal.feature.chat.presentation.AccountUi
import co.ratmo.anreal.feature.chat.presentation.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AccountSection {
    Account,
    Usage,
    Personalization,
}

data class AccountUsageUi(
    val storageUsed: String,
    val storageMax: String,
    val storageFraction: Float,
    val requestCount: String,
    val totalTokens: String,
    val inputTokens: String,
    val outputTokens: String,
    val cachedTokens: String,
    val models: List<UsageBreakdownUi>,
    val reasoning: List<UsageBreakdownUi>,
)

data class UsageBreakdownUi(
    val label: String,
    val requests: String,
    val tokens: String,
)

data class ProfileSectionUi(
    val key: String,
    val label: String,
    val bullets: List<String>,
)

data class ProfileUi(
    val sections: List<ProfileSectionUi>,
    val explicitFacts: List<String>,
    val updatedAt: String,
) {
    val isEmpty: Boolean get() = sections.all { it.bullets.isEmpty() } && explicitFacts.isEmpty()
}

data class ProjectProfileUi(
    val id: String,
    val name: String,
    val profile: ProfileUi?,
)

sealed interface ProfileResetTarget {
    data object User : ProfileResetTarget
    data class Project(val id: String, val name: String) : ProfileResetTarget
}

@Stable
data class AccountState(
    val name: String = "",
    val email: String = "",
    val section: AccountSection = AccountSection.Account,
    val isSigningOut: Boolean = false,
    val isHealthLoading: Boolean = true,
    val isApiHealthy: Boolean? = null,
    val usage: AccountUsageUi? = null,
    val isUsageLoading: Boolean = false,
    val usageError: UiText? = null,
    val userProfile: ProfileUi? = null,
    val projectProfiles: List<ProjectProfileUi> = emptyList(),
    val isPersonalizationLoading: Boolean = false,
    val personalizationError: UiText? = null,
    val resetTarget: ProfileResetTarget? = null,
    val isResettingProfile: Boolean = false,
    val resetError: UiText? = null,
    val themeMode: AppThemeMode = AppThemeMode.System,
    val dynamicColor: Boolean = true,
    val reduceMotion: Boolean = false,
    val reduceTransparency: Boolean = false,
)

sealed interface AccountAction {
    data class OnSelectSection(val section: AccountSection) : AccountAction
    data object OnRetryUsage : AccountAction
    data object OnRetryHealth : AccountAction
    data object OnRetryPersonalization : AccountAction
    data object OnRequestResetUserProfile : AccountAction
    data class OnRequestResetProjectProfile(val id: String, val name: String) : AccountAction
    data object OnConfirmResetProfile : AccountAction
    data object OnDismissResetProfile : AccountAction
    data object OnBack : AccountAction
    data object OnSignOut : AccountAction
    data class OnThemeModeChange(val mode: AppThemeMode) : AccountAction
    data object OnToggleDynamicColor : AccountAction
    data object OnToggleReduceMotion : AccountAction
    data object OnToggleReduceTransparency : AccountAction
}

sealed interface AccountEvent {
    data object NavigateBack : AccountEvent
    data object SignOut : AccountEvent
    data class ShowMessage(val message: UiText) : AccountEvent
}

class AccountViewModel(
    account: AccountUi,
    private val settingsDataSource: AccountSettingsDataSource,
    private val preferencesRepository: AppPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AccountState(name = account.name, email = account.email),
    )
    val state = _state.asStateFlow()

    private val _events = Channel<AccountEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { preferences ->
                _state.update {
                    it.copy(
                        themeMode = preferences.themeMode,
                        dynamicColor = preferences.dynamicColor,
                        reduceMotion = preferences.reduceMotion,
                        reduceTransparency = preferences.reduceTransparency,
                    )
                }
            }
        }
        viewModelScope.launch { checkHealth() }
    }

    fun onAction(action: AccountAction) {
        when (action) {
            is AccountAction.OnSelectSection -> {
                _state.update { it.copy(section = action.section) }
                when (action.section) {
                    AccountSection.Account -> Unit
                    AccountSection.Usage -> if (_state.value.usage == null) {
                        viewModelScope.launch { loadUsage() }
                    }
                    AccountSection.Personalization -> if (
                        _state.value.userProfile == null && _state.value.projectProfiles.isEmpty()
                    ) {
                        viewModelScope.launch { loadPersonalization() }
                    }
                }
            }
            AccountAction.OnRetryUsage -> viewModelScope.launch { loadUsage() }
            AccountAction.OnRetryHealth -> viewModelScope.launch { checkHealth() }
            AccountAction.OnRetryPersonalization -> viewModelScope.launch { loadPersonalization() }
            AccountAction.OnRequestResetUserProfile -> _state.update {
                it.copy(resetTarget = ProfileResetTarget.User, resetError = null)
            }
            is AccountAction.OnRequestResetProjectProfile -> _state.update {
                it.copy(
                    resetTarget = ProfileResetTarget.Project(action.id, action.name),
                    resetError = null,
                )
            }
            AccountAction.OnConfirmResetProfile -> viewModelScope.launch { resetProfile() }
            AccountAction.OnDismissResetProfile -> if (!_state.value.isResettingProfile) {
                _state.update { it.copy(resetTarget = null, resetError = null) }
            }
            AccountAction.OnBack -> viewModelScope.launch {
                _events.send(AccountEvent.NavigateBack)
            }
            AccountAction.OnSignOut -> {
                if (_state.value.isSigningOut) return
                _state.update { it.copy(isSigningOut = true) }
                viewModelScope.launch { _events.send(AccountEvent.SignOut) }
            }
            is AccountAction.OnThemeModeChange -> viewModelScope.launch {
                preferencesRepository.setThemeMode(action.mode)
            }
            AccountAction.OnToggleDynamicColor -> viewModelScope.launch {
                preferencesRepository.setDynamicColor(!_state.value.dynamicColor)
            }
            AccountAction.OnToggleReduceMotion -> viewModelScope.launch {
                preferencesRepository.setReduceMotion(!_state.value.reduceMotion)
            }
            AccountAction.OnToggleReduceTransparency -> viewModelScope.launch {
                preferencesRepository.setReduceTransparency(!_state.value.reduceTransparency)
            }
        }
    }

    private suspend fun checkHealth() {
        if (_state.value.isHealthLoading && _state.value.isApiHealthy != null) return
        _state.update { it.copy(isHealthLoading = true) }
        when (val result = settingsDataSource.checkHealth()) {
            is Result.Success -> _state.update {
                it.copy(isHealthLoading = false, isApiHealthy = result.data)
            }
            is Result.Error -> _state.update {
                it.copy(isHealthLoading = false, isApiHealthy = false)
            }
        }
    }

    private suspend fun loadUsage() {
        if (_state.value.isUsageLoading) return
        _state.update { it.copy(isUsageLoading = true, usageError = null) }
        when (val result = settingsDataSource.loadUsage()) {
            is Result.Success -> _state.update {
                it.copy(usage = result.data.toUi(), isUsageLoading = false)
            }
            is Result.Error -> _state.update {
                it.copy(isUsageLoading = false, usageError = result.error.toUiText())
            }
        }
    }

    private suspend fun loadPersonalization() {
        if (_state.value.isPersonalizationLoading) return
        _state.update { it.copy(isPersonalizationLoading = true, personalizationError = null) }
        when (val result = settingsDataSource.loadPersonalization()) {
            is Result.Success -> _state.update {
                it.copy(
                    userProfile = result.data.user?.toUi(),
                    projectProfiles = result.data.projects.map { project ->
                        ProjectProfileUi(project.id, project.name, project.profile?.toUi())
                    },
                    isPersonalizationLoading = false,
                )
            }
            is Result.Error -> _state.update {
                it.copy(
                    isPersonalizationLoading = false,
                    personalizationError = result.error.toUiText(),
                )
            }
        }
    }

    private suspend fun resetProfile() {
        val target = _state.value.resetTarget ?: return
        if (_state.value.isResettingProfile) return
        _state.update { it.copy(isResettingProfile = true, resetError = null) }
        val result = when (target) {
            ProfileResetTarget.User -> settingsDataSource.resetUserPersonalization()
            is ProfileResetTarget.Project -> settingsDataSource.resetProjectPersonalization(target.id)
        }
        when (result) {
            is Result.Success -> {
                _state.update { it.copy(isResettingProfile = false, resetTarget = null) }
                loadPersonalization()
                _events.send(
                    AccountEvent.ShowMessage(UiText.StringResource(AnrealCopy.TOAST_PROFILE_RESET)),
                )
            }
            is Result.Error -> _state.update {
                it.copy(isResettingProfile = false, resetError = result.error.toUiText())
            }
        }
    }
}

private fun UsageSummary.toUi(): AccountUsageUi = AccountUsageUi(
    storageUsed = storage.usedBytes.toFileSize(),
    storageMax = storage.maxBytes.toFileSize(),
    storageFraction = if (storage.maxBytes <= 0L) 0f else {
        (storage.usedBytes.toDouble() / storage.maxBytes.toDouble()).toFloat().coerceIn(0f, 1f)
    },
    requestCount = tokens.requestCount.toCount(),
    totalTokens = tokens.totalTokens.toCount(),
    inputTokens = tokens.inputTokens.toCount(),
    outputTokens = tokens.outputTokens.toCount(),
    cachedTokens = tokens.cachedInputTokens.toCount(),
    models = byModel.map { UsageBreakdownUi(it.model, it.requestCount.toCount(), it.totalTokens.toCount()) },
    reasoning = byReasoningEffort.map {
        UsageBreakdownUi(
            it.reasoningEffort.replaceFirstChar { char -> char.uppercase() },
            it.requestCount.toCount(),
            it.totalTokens.toCount(),
        )
    },
)

private fun PersonalizationProfile.toUi(): ProfileUi = ProfileUi(
    sections = sections.map { (key, bullets) ->
        ProfileSectionUi(
            key = key,
            label = key.replaceFirstChar { char -> char.uppercase() },
            bullets = bullets.map { it.text },
        )
    },
    explicitFacts = explicitFacts.map { it.fact },
    updatedAt = updatedAt,
)

private fun Long.toCount(): String = toString().reversed().chunked(3).joinToString(",").reversed()

private fun Long.toFileSize(): String {
    val mib = this / (1024.0 * 1024.0)
    return if (mib >= 1.0) {
        val rounded = (mib * 10).toLong() / 10.0
        "$rounded MB"
    } else {
        val kib = (this / 1024.0).toLong()
        "$kib KB"
    }
}
