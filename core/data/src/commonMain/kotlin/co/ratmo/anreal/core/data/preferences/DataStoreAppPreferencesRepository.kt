package co.ratmo.anreal.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.ratmo.anreal.core.domain.model.AppPreferences
import co.ratmo.anreal.core.domain.model.AppPreferencesRepository
import co.ratmo.anreal.core.domain.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreAppPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : AppPreferencesRepository {
    override val preferences: Flow<AppPreferences> = dataStore.data.map { values ->
        AppPreferences(
            themeMode = values[THEME]?.let { stored ->
                AppThemeMode.entries.firstOrNull { it.name == stored }
            } ?: AppThemeMode.System,
            dynamicColor = values[DYNAMIC_COLOR] ?: true,
            reduceMotion = values[REDUCE_MOTION] ?: false,
            reduceTransparency = values[REDUCE_TRANSPARENCY] ?: false,
        )
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { it[THEME] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setReduceMotion(enabled: Boolean) {
        dataStore.edit { it[REDUCE_MOTION] = enabled }
    }

    override suspend fun setReduceTransparency(enabled: Boolean) {
        dataStore.edit { it[REDUCE_TRANSPARENCY] = enabled }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val REDUCE_TRANSPARENCY = booleanPreferencesKey("reduce_transparency")
    }
}
