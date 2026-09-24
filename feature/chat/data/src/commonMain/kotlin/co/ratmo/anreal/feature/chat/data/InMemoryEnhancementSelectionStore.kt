package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.feature.chat.domain.EnhancementSelectionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [EnhancementSelectionStore] for stub environments and previews.
 * Mirrors the test fake's behavior: saves replace the observed value.
 */
class InMemoryEnhancementSelectionStore(
    initialSkills: List<String> = emptyList(),
    initialMcp: List<String> = emptyList(),
) : EnhancementSelectionStore {
    private val skillIds = MutableStateFlow(initialSkills)
    private val mcpIds = MutableStateFlow(initialMcp)

    override fun observeSkillIds(): Flow<List<String>> = skillIds

    override fun observeMcpServerIds(): Flow<List<String>> = mcpIds

    override suspend fun saveSkillIds(ids: List<String>) {
        skillIds.value = ids
    }

    override suspend fun saveMcpServerIds(ids: List<String>) {
        mcpIds.value = ids
    }
}
