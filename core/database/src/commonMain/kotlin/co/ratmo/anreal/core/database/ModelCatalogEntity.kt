package co.ratmo.anreal.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "model_catalog_models")
data class ModelCatalogModelEntity(
    @PrimaryKey val id: String,
    val label: String,
    val contextWindowTokens: Int,
    val position: Int,
    @ColumnInfo(defaultValue = "'text'") val outputType: String = "text",
    @ColumnInfo(defaultValue = "''") val providerName: String = "",
    val hint: String? = null,
    val description: String? = null,
    val maxInputTokens: Int? = null,
    val maxOutputTokens: Int? = null,
    val priceInput: Double? = null,
    val priceCachedInput: Double? = null,
    val priceOutput: Double? = null,
    @ColumnInfo(defaultValue = "''") val inputModalities: String = "",
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
