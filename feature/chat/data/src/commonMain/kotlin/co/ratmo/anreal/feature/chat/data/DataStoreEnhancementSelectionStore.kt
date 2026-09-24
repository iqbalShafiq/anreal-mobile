package co.ratmo.anreal.feature.chat.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.ratmo.anreal.feature.chat.domain.EnhancementSelectionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val SkillsSelectionKey = stringPreferencesKey("skills_selection")
private val McpSelectionKey = stringPreferencesKey("mcp_selection")

class DataStoreEnhancementSelectionStore(
    private val dataStore: DataStore<Preferences>,
) : EnhancementSelectionStore {

    override fun observeSkillIds(): Flow<List<String>> =
        dataStore.data.map { preferences -> parseIds(preferences[SkillsSelectionKey]) }

    override fun observeMcpServerIds(): Flow<List<String>> =
        dataStore.data.map { preferences -> parseIds(preferences[McpSelectionKey]) }

    override suspend fun saveSkillIds(ids: List<String>) {
        dataStore.edit { preferences ->
            preferences[SkillsSelectionKey] = joinIds(ids, MAX_SKILL_IDS)
        }
    }

    override suspend fun saveMcpServerIds(ids: List<String>) {
        dataStore.edit { preferences ->
            preferences[McpSelectionKey] = joinIds(ids, MAX_MCP_SERVER_IDS)
        }
    }

    private companion object {
        const val MAX_SKILL_IDS = 20
        const val MAX_MCP_SERVER_IDS = 5
    }
}

private fun parseIds(raw: String?): List<String> =
    raw.orEmpty().split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct()

private fun joinIds(ids: List<String>, limit: Int): String =
    ids.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(limit).joinToString(",")
