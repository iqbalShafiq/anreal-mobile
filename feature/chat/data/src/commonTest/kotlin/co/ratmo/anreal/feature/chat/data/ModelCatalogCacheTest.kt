package co.ratmo.anreal.feature.chat.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import co.ratmo.anreal.core.data.network.HttpClientFactory
import co.ratmo.anreal.core.database.MessageDao
import co.ratmo.anreal.core.database.MessageEntity
import co.ratmo.anreal.core.database.ModelCatalogDao
import co.ratmo.anreal.core.database.ModelCatalogEffortEntity
import co.ratmo.anreal.core.database.ModelCatalogMetadataEntity
import co.ratmo.anreal.core.database.ModelCatalogModelEffortEntity
import co.ratmo.anreal.core.database.ModelCatalogModelEntity
import co.ratmo.anreal.core.database.QueuedItemDao
import co.ratmo.anreal.core.database.QueuedItemEntity
import co.ratmo.anreal.core.database.SessionDao
import co.ratmo.anreal.core.database.SessionEntity
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.ChatModel
import co.ratmo.anreal.feature.chat.domain.ModelCatalog
import co.ratmo.anreal.feature.chat.domain.ReasoningEffort
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ModelCatalogCacheTest {

    @Test
    fun empty_database_emits_no_cached_catalog() = runTest {
        val source = RoomModelCatalogLocalDataSource(FakeModelCatalogDao())

        assertThat(source.observeCachedCatalog().first()).isEqualTo(null)
    }

    @Test
    fun replace_catalog_maps_normalized_rows_and_preserves_order() = runTest {
        val dao = FakeModelCatalogDao()
        val source = RoomModelCatalogLocalDataSource(dao)
        val catalog = catalog(
            models = listOf(
                ChatModel("slow", "Slow", listOf("high", "low"), 200_000),
                ChatModel("fast", "Fast", listOf("none"), 128_000),
            ),
            efforts = listOf(
                ReasoningEffort("none", "None"),
                ReasoningEffort("high", "High", "Deep reasoning"),
                ReasoningEffort("low", "Low"),
            ),
        )

        source.replaceCatalog(catalog)

        assertThat(dao.models.value).isEqualTo(
            listOf(
                ModelCatalogModelEntity("slow", "Slow", 200_000, 0),
                ModelCatalogModelEntity("fast", "Fast", 128_000, 1),
            ),
        )
        assertThat(dao.efforts.value).isEqualTo(
            listOf(
                ModelCatalogEffortEntity("none", "None", null, 0),
                ModelCatalogEffortEntity("high", "High", "Deep reasoning", 1),
                ModelCatalogEffortEntity("low", "Low", null, 2),
            ),
        )
        assertThat(dao.modelEfforts.value).isEqualTo(
            listOf(
                ModelCatalogModelEffortEntity("slow", "high", 0),
                ModelCatalogModelEffortEntity("slow", "low", 1),
                ModelCatalogModelEffortEntity("fast", "none", 0),
            ),
        )
        assertThat(source.observeCachedCatalog().first()?.catalog).isEqualTo(catalog)
    }

    @Test
    fun persist_selection_changes_only_metadata() = runTest {
        val dao = FakeModelCatalogDao()
        val source = RoomModelCatalogLocalDataSource(dao)
        source.replaceCatalog(catalog())
        val modelsBefore = dao.models.value
        val effortsBefore = dao.efforts.value
        val relationsBefore = dao.modelEfforts.value
        val refreshBefore = dao.metadata.value?.lastSuccessfulRefreshEpochMillis

        source.persistSelection("fast", "high")

        assertThat(dao.models.value).isEqualTo(modelsBefore)
        assertThat(dao.efforts.value).isEqualTo(effortsBefore)
        assertThat(dao.modelEfforts.value).isEqualTo(relationsBefore)
        assertThat(dao.metadata.value).isEqualTo(
            ModelCatalogMetadataEntity(
                lastSuccessfulRefreshEpochMillis = refreshBefore,
                selectedModelId = "fast",
                selectedReasoningEffort = "high",
            ),
        )
    }

    @Test
    fun successful_remote_refresh_becomes_observable_in_local_cache() = runTest {
        val local = RoomModelCatalogLocalDataSource(FakeModelCatalogDao())
        val repository = repository(
            remoteBody = """
                {
                  "models":[{"modelId":"gpt-luna","label":"GPT Luna","reasoningEfforts":["low","high"],"contextWindowTokens":200000}],
                  "reasoningEfforts":[{"key":"low","label":"Low"},{"key":"high","label":"High"}]
                }
            """.trimIndent(),
            local = local,
        )

        val result = repository.loadCatalog()

        assertThat(result).isEqualTo(
            Result.Success(
                catalog(
                    models = listOf(ChatModel("gpt-luna", "GPT Luna", listOf("low", "high"), 200_000)),
                    efforts = listOf(ReasoningEffort("low", "Low"), ReasoningEffort("high", "High")),
                ),
            ),
        )
        val refreshedCatalog = when (result) {
            is Result.Success -> result.data
            is Result.Error -> error("expected catalog refresh to succeed")
        }
        assertThat(local.observeCachedCatalog().first()?.catalog).isEqualTo(refreshedCatalog)
        assertThat(local.observeCachedCatalog().first()?.lastSuccessfulRefreshEpochMillis != null).isEqualTo(true)
    }

    @Test
    fun failed_remote_refresh_preserves_last_good_cache() = runTest {
        val local = RoomModelCatalogLocalDataSource(FakeModelCatalogDao())
        local.replaceCatalog(catalog())
        val before = local.observeCachedCatalog().first()
        val repository = repository(
            remoteStatus = HttpStatusCode.InternalServerError,
            remoteBody = """{"error":"catalog unavailable"}""",
            local = local,
        )

        val result = repository.loadCatalog()

        when (result) {
            is Result.Success -> error("expected catalog refresh to fail")
            is Result.Error -> Unit
        }
        assertThat(local.observeCachedCatalog().first()).isEqualTo(before)
    }
}

private fun catalog(
    models: List<ChatModel> = listOf(ChatModel("fast", "Fast", listOf("high"), 128_000)),
    efforts: List<ReasoningEffort> = listOf(ReasoningEffort("high", "High")),
) = ModelCatalog(models = models, efforts = efforts)

private fun repository(
    remoteStatus: HttpStatusCode = HttpStatusCode.OK,
    remoteBody: String,
    local: RoomModelCatalogLocalDataSource,
): OfflineFirstChatRepository {
    val engine = MockEngine {
        check(it.url.encodedPath == "/api/models")
        respond(
            content = remoteBody,
            status = remoteStatus,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }
    val remote = KtorChatRemoteDataSource(
        HttpClientFactory.create(
            engine = engine,
            tokenStore = InMemorySessionTokenStore(),
            baseUrl = "http://127.0.0.1:3001",
        ),
    )
    return OfflineFirstChatRepository(remote, emptyRoomChatLocalDataSource(), local)
}

private class FakeModelCatalogDao : ModelCatalogDao {
    val models = MutableStateFlow<List<ModelCatalogModelEntity>>(emptyList())
    val efforts = MutableStateFlow<List<ModelCatalogEffortEntity>>(emptyList())
    val modelEfforts = MutableStateFlow<List<ModelCatalogModelEffortEntity>>(emptyList())
    val metadata = MutableStateFlow<ModelCatalogMetadataEntity?>(null)

    override fun observeModels(): Flow<List<ModelCatalogModelEntity>> = models

    override fun observeEfforts(): Flow<List<ModelCatalogEffortEntity>> = efforts

    override fun observeModelEfforts(): Flow<List<ModelCatalogModelEffortEntity>> = modelEfforts

    override fun observeMetadata(): Flow<ModelCatalogMetadataEntity?> = metadata

    override suspend fun getMetadata(): ModelCatalogMetadataEntity? = metadata.value

    override suspend fun deleteModels() {
        models.value = emptyList()
    }

    override suspend fun deleteEfforts() {
        efforts.value = emptyList()
    }

    override suspend fun deleteModelEfforts() {
        modelEfforts.value = emptyList()
    }

    override suspend fun upsertModels(models: List<ModelCatalogModelEntity>) {
        this.models.value = models
    }

    override suspend fun upsertEfforts(efforts: List<ModelCatalogEffortEntity>) {
        this.efforts.value = efforts
    }

    override suspend fun upsertModelEfforts(modelEfforts: List<ModelCatalogModelEffortEntity>) {
        this.modelEfforts.value = modelEfforts
    }

    override suspend fun upsertMetadata(metadata: ModelCatalogMetadataEntity) {
        this.metadata.value = metadata
    }

    override suspend fun replaceCatalog(
        models: List<ModelCatalogModelEntity>,
        efforts: List<ModelCatalogEffortEntity>,
        modelEfforts: List<ModelCatalogModelEffortEntity>,
        lastSuccessfulRefreshEpochMillis: Long?,
    ) {
        this.models.value = models
        this.efforts.value = efforts
        this.modelEfforts.value = modelEfforts
        val existing = metadata.value
        metadata.value = ModelCatalogMetadataEntity(
            lastSuccessfulRefreshEpochMillis = lastSuccessfulRefreshEpochMillis,
            selectedModelId = existing?.selectedModelId,
            selectedReasoningEffort = existing?.selectedReasoningEffort,
        )
    }

    override suspend fun upsertSelection(modelId: String?, reasoningEffort: String?) {
        val existing = metadata.value
        metadata.value = ModelCatalogMetadataEntity(
            lastSuccessfulRefreshEpochMillis = existing?.lastSuccessfulRefreshEpochMillis,
            selectedModelId = modelId,
            selectedReasoningEffort = reasoningEffort,
        )
    }
}

private fun emptyRoomChatLocalDataSource() = RoomChatLocalDataSource(
    sessionDao = EmptySessionDao,
    messageDao = EmptyMessageDao,
    queuedItemDao = EmptyQueuedItemDao,
)

private object EmptySessionDao : SessionDao {
    override fun observeStandalone(): Flow<List<SessionEntity>> = MutableStateFlow(emptyList())
    override fun observeForProject(projectId: String): Flow<List<SessionEntity>> = MutableStateFlow(emptyList())
    override suspend fun getSession(id: String): SessionEntity? = null
    override suspend fun upsert(sessions: List<SessionEntity>) = Unit
    override suspend fun delete(id: String) = Unit
    override suspend fun updateResume(id: String, streamId: String?, lastEventId: Int) = Unit
}

private object EmptyMessageDao : MessageDao {
    override suspend fun getMessages(sessionId: String): List<MessageEntity> = emptyList()
    override suspend fun getLatestMessages(sessionId: String, limit: Int): List<MessageEntity> = emptyList()
    override suspend fun getMessagesBefore(sessionId: String, beforePosition: Int, limit: Int): List<MessageEntity> = emptyList()
    override suspend fun countMessages(sessionId: String): Int = 0
    override suspend fun upsert(messages: List<MessageEntity>) = Unit
    override suspend fun deleteForSession(sessionId: String) = Unit
    override suspend fun deleteFromPosition(sessionId: String, fromPosition: Int) = Unit
}

private object EmptyQueuedItemDao : QueuedItemDao {
    override suspend fun getForSession(sessionId: String): List<QueuedItemEntity> = emptyList()
    override suspend fun upsert(items: List<QueuedItemEntity>) = Unit
    override suspend fun deleteForSession(sessionId: String) = Unit
}
