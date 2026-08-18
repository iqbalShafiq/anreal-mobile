package co.ratmo.anreal.core.data.preferences

import co.ratmo.anreal.core.domain.model.AppPreferences
import co.ratmo.anreal.core.domain.model.AppPreferencesRepository
import co.ratmo.anreal.core.domain.model.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.module.Module
import org.koin.dsl.module

actual val appPreferencesModule: Module = module {
    single<AppPreferencesRepository> { InMemoryAppPreferencesRepository() }
}

private class InMemoryAppPreferencesRepository : AppPreferencesRepository {
    override val preferences = MutableStateFlow(AppPreferences())

    override suspend fun setThemeMode(mode: AppThemeMode) {
        preferences.value = preferences.value.copy(themeMode = mode)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        preferences.value = preferences.value.copy(dynamicColor = enabled)
    }

    override suspend fun setReduceMotion(enabled: Boolean) {
        preferences.value = preferences.value.copy(reduceMotion = enabled)
    }

    override suspend fun setReduceTransparency(enabled: Boolean) {
        preferences.value = preferences.value.copy(reduceTransparency = enabled)
    }

    override suspend fun setChatModel(modelId: String?) {
        preferences.value = preferences.value.copy(chatModelId = modelId)
    }

    override suspend fun setChatReasoningEffort(effort: String?) {
        preferences.value = preferences.value.copy(chatReasoningEffort = effort)
    }
}
