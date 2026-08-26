package co.ratmo.anreal.core.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelCatalogDao {
    @Query("SELECT * FROM model_catalog_models ORDER BY position ASC")
    fun observeModels(): Flow<List<ModelCatalogModelEntity>>

    @Query("SELECT * FROM model_catalog_efforts ORDER BY position ASC")
    fun observeEfforts(): Flow<List<ModelCatalogEffortEntity>>

    @Query("SELECT * FROM model_catalog_model_efforts ORDER BY modelId ASC, position ASC")
    fun observeModelEfforts(): Flow<List<ModelCatalogModelEffortEntity>>

    @Query("SELECT * FROM model_catalog_metadata WHERE id = 1 LIMIT 1")
    fun observeMetadata(): Flow<ModelCatalogMetadataEntity?>

    @Query("SELECT * FROM model_catalog_metadata WHERE id = 1 LIMIT 1")
    suspend fun getMetadata(): ModelCatalogMetadataEntity?

    @Query("DELETE FROM model_catalog_models")
    suspend fun deleteModels()

    @Query("DELETE FROM model_catalog_efforts")
    suspend fun deleteEfforts()

    @Query("DELETE FROM model_catalog_model_efforts")
    suspend fun deleteModelEfforts()

    @Upsert
    suspend fun upsertModels(models: List<ModelCatalogModelEntity>)

    @Upsert
    suspend fun upsertEfforts(efforts: List<ModelCatalogEffortEntity>)

    @Upsert
    suspend fun upsertModelEfforts(modelEfforts: List<ModelCatalogModelEffortEntity>)

    @Upsert
    suspend fun upsertMetadata(metadata: ModelCatalogMetadataEntity)

    @Transaction
    suspend fun replaceCatalog(
        models: List<ModelCatalogModelEntity>,
        efforts: List<ModelCatalogEffortEntity>,
        modelEfforts: List<ModelCatalogModelEffortEntity>,
        lastSuccessfulRefreshEpochMillis: Long?,
    ) {
        deleteModels()
        deleteEfforts()
        deleteModelEfforts()
        upsertModels(models)
        upsertEfforts(efforts)
        upsertModelEfforts(modelEfforts)

        val existingMetadata = getMetadata()
        upsertMetadata(
            ModelCatalogMetadataEntity(
                lastSuccessfulRefreshEpochMillis = lastSuccessfulRefreshEpochMillis,
                selectedModelId = existingMetadata?.selectedModelId,
                selectedReasoningEffort = existingMetadata?.selectedReasoningEffort,
            ),
        )
    }

    @Transaction
    suspend fun upsertSelection(modelId: String?, reasoningEffort: String?) {
        val existingMetadata = getMetadata()
        upsertMetadata(
            ModelCatalogMetadataEntity(
                lastSuccessfulRefreshEpochMillis = existingMetadata?.lastSuccessfulRefreshEpochMillis,
                selectedModelId = modelId,
                selectedReasoningEffort = reasoningEffort,
            ),
        )
    }
}
