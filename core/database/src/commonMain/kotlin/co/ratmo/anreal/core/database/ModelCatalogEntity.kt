package co.ratmo.anreal.core.database

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "model_catalog_models")
data class ModelCatalogModelEntity(
    @PrimaryKey val id: String,
    val label: String,
    val contextWindowTokens: Int,
    val position: Int,
)

@Entity(tableName = "model_catalog_efforts")
data class ModelCatalogEffortEntity(
    @PrimaryKey val key: String,
    val label: String,
    val description: String?,
    val position: Int,
)

@Entity(
    tableName = "model_catalog_model_efforts",
    primaryKeys = ["modelId", "effortKey"],
)
data class ModelCatalogModelEffortEntity(
    val modelId: String,
    val effortKey: String,
    val position: Int,
)

@Entity(tableName = "model_catalog_metadata")
data class ModelCatalogMetadataEntity(
    @PrimaryKey val id: Int = 1,
    val lastSuccessfulRefreshEpochMillis: Long?,
    val selectedModelId: String?,
    val selectedReasoningEffort: String?,
)
