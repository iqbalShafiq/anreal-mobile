package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.database.ModelCatalogDao
import co.ratmo.anreal.core.database.ModelCatalogEffortEntity
import co.ratmo.anreal.core.database.ModelCatalogModelEffortEntity
import co.ratmo.anreal.core.database.ModelCatalogModelEntity
import co.ratmo.anreal.feature.chat.domain.CachedModelCatalog
import co.ratmo.anreal.feature.chat.domain.ChatModel
import co.ratmo.anreal.feature.chat.domain.ModelCatalog
import co.ratmo.anreal.feature.chat.domain.ReasoningEffort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class RoomModelCatalogLocalDataSource(
    private val dao: ModelCatalogDao,
) {
    fun observeCachedCatalog(): Flow<CachedModelCatalog?> {
        return combine(
            dao.observeModels(),
            dao.observeEfforts(),
            dao.observeModelEfforts(),
            dao.observeMetadata(),
        ) { modelRows, effortRows, relationRows, metadata ->
            if (metadata == null && modelRows.isEmpty()) {
                null
            } else {
                CachedModelCatalog(
                    catalog = ModelCatalog(
                        models = modelRows
                            .sortedBy { it.position }
                            .map { model ->
                                ChatModel(
                                    id = model.id,
                                    label = model.label,
                                    reasoningEfforts = relationRows
                                        .asSequence()
                                        .filter { it.modelId == model.id }
                                        .sortedBy { it.position }
                                        .map { it.effortKey }
                                        .toList(),
                                    contextWindowTokens = model.contextWindowTokens,
                                )
                            },
                        efforts = effortRows
                            .sortedBy { it.position }
                            .map { effort ->
                                ReasoningEffort(
                                    key = effort.key,
                                    label = effort.label,
                                    description = effort.description,
                                )
                            },
                    ),
                    selectedModelId = metadata?.selectedModelId,
                    selectedReasoningEffort = metadata?.selectedReasoningEffort,
                    lastSuccessfulRefreshEpochMillis = metadata?.lastSuccessfulRefreshEpochMillis,
                )
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun replaceCatalog(catalog: ModelCatalog) {
        dao.replaceCatalog(
            models = catalog.models.mapIndexed { position, model ->
                ModelCatalogModelEntity(
                    id = model.id,
                    label = model.label,
                    contextWindowTokens = model.contextWindowTokens,
                    position = position,
                )
            },
            efforts = catalog.efforts.mapIndexed { position, effort ->
                ModelCatalogEffortEntity(
                    key = effort.key,
                    label = effort.label,
                    description = effort.description,
                    position = position,
                )
            },
            modelEfforts = catalog.models.flatMap { model ->
                model.reasoningEfforts.mapIndexed { position, effortKey ->
                    ModelCatalogModelEffortEntity(
                        modelId = model.id,
                        effortKey = effortKey,
                        position = position,
                    )
                }
            },
            lastSuccessfulRefreshEpochMillis = Clock.System.now().toEpochMilliseconds(),
        )
    }

    suspend fun persistSelection(modelId: String?, reasoningEffort: String?) {
        dao.upsertSelection(modelId, reasoningEffort)
    }
}
