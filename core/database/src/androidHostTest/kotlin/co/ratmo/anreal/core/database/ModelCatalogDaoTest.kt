package co.ratmo.anreal.core.database

import android.content.Context
import androidx.room3.Room
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModelCatalogDaoTest {
    private lateinit var database: AnrealDatabase
    private lateinit var dao: ModelCatalogDao

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        database = getRoomDatabase(Room.inMemoryDatabaseBuilder<AnrealDatabase>(context))
        dao = database.modelCatalogDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun replaceCatalog_persists_normalized_rows_and_metadata() = runBlocking {
        val models = listOf(
            ModelCatalogModelEntity("gpt-luna", "GPT Luna", 128_000, 0),
            ModelCatalogModelEntity("gpt-sol", "GPT Sol", 256_000, 1),
        )
        val efforts = listOf(
            ModelCatalogEffortEntity("none", "None", null, 0),
            ModelCatalogEffortEntity("high", "High", "Deep reasoning", 1),
        )
        val modelEfforts = listOf(
            ModelCatalogModelEffortEntity("gpt-luna", "none", 0),
            ModelCatalogModelEffortEntity("gpt-luna", "high", 1),
            ModelCatalogModelEffortEntity("gpt-sol", "none", 0),
        )

        dao.replaceCatalog(models, efforts, modelEfforts, lastSuccessfulRefreshEpochMillis = 1234L)

        assertThat(dao.observeModels().first()).containsExactly(*models.toTypedArray())
        assertThat(dao.observeEfforts().first()).containsExactly(*efforts.toTypedArray())
        assertThat(dao.observeModelEfforts().first()).containsExactly(*modelEfforts.toTypedArray())
        assertThat(dao.observeMetadata().first()).isEqualTo(
            ModelCatalogMetadataEntity(
                lastSuccessfulRefreshEpochMillis = 1234L,
                selectedModelId = null,
                selectedReasoningEffort = null,
            ),
        )
    }

    @Test
    fun replaceCatalog_with_empty_catalog_removes_old_rows() = runBlocking {
        dao.replaceCatalog(
            models = listOf(ModelCatalogModelEntity("gpt-luna", "GPT Luna", 128_000, 0)),
            efforts = listOf(ModelCatalogEffortEntity("none", "None", null, 0)),
            modelEfforts = listOf(ModelCatalogModelEffortEntity("gpt-luna", "none", 0)),
            lastSuccessfulRefreshEpochMillis = 1234L,
        )
        dao.upsertSelection("gpt-luna", "none")

        dao.replaceCatalog(
            models = emptyList(),
            efforts = emptyList(),
            modelEfforts = emptyList(),
            lastSuccessfulRefreshEpochMillis = 5678L,
        )

        assertThat(dao.observeModels().first()).isEqualTo(emptyList())
        assertThat(dao.observeEfforts().first()).isEqualTo(emptyList())
        assertThat(dao.observeModelEfforts().first()).isEqualTo(emptyList())
        assertThat(dao.observeMetadata().first()).isEqualTo(
            ModelCatalogMetadataEntity(
                lastSuccessfulRefreshEpochMillis = 5678L,
                selectedModelId = "gpt-luna",
                selectedReasoningEffort = "none",
            ),
        )
    }

    @Test
    fun upsertSelection_updates_metadata_without_touching_catalog_rows() = runBlocking {
        val models = listOf(ModelCatalogModelEntity("gpt-luna", "GPT Luna", 128_000, 0))
        val efforts = listOf(ModelCatalogEffortEntity("high", "High", null, 0))
        val modelEfforts = listOf(ModelCatalogModelEffortEntity("gpt-luna", "high", 0))
        dao.replaceCatalog(models, efforts, modelEfforts, lastSuccessfulRefreshEpochMillis = 1234L)

        dao.upsertSelection("gpt-luna", "high")

        assertThat(dao.observeModels().first()).containsExactly(*models.toTypedArray())
        assertThat(dao.observeEfforts().first()).containsExactly(*efforts.toTypedArray())
        assertThat(dao.observeModelEfforts().first()).containsExactly(*modelEfforts.toTypedArray())
        assertThat(dao.observeMetadata().first()).isEqualTo(
            ModelCatalogMetadataEntity(
                lastSuccessfulRefreshEpochMillis = 1234L,
                selectedModelId = "gpt-luna",
                selectedReasoningEffort = "high",
            ),
        )

        dao.upsertSelection(null, null)
        assertThat(dao.observeMetadata().first()?.selectedModelId).isNull()
    }
}
