package co.ratmo.anreal.core.domain.model

import kotlinx.coroutines.flow.Flow

enum class AppThemeMode { System, Light, Dark }

data class AppPreferences(
    val themeMode: AppThemeMode = AppThemeMode.System,
    val dynamicColor: Boolean = true,
    val reduceMotion: Boolean = false,
    val reduceTransparency: Boolean = false,
    val chatModelId: String? = null,
    val chatReasoningEffort: String? = null,
)

interface AppPreferencesRepository {
    val preferences: Flow<AppPreferences>
    suspend fun setThemeMode(mode: AppThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setReduceMotion(enabled: Boolean)
    suspend fun setReduceTransparency(enabled: Boolean)
    suspend fun setChatModel(modelId: String?)
    suspend fun setChatReasoningEffort(effort: String?)
}
