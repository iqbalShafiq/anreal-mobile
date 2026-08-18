package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.feature.chat.domain.ExplicitProfileFact
import co.ratmo.anreal.feature.chat.domain.ModelUsage
import co.ratmo.anreal.feature.chat.domain.PersonalizationProfile
import co.ratmo.anreal.feature.chat.domain.PersonalizationSettings
import co.ratmo.anreal.feature.chat.domain.ProfileBullet
import co.ratmo.anreal.feature.chat.domain.ProjectPersonalization
import co.ratmo.anreal.feature.chat.domain.ReasoningUsage
import co.ratmo.anreal.feature.chat.domain.StorageUsage
import co.ratmo.anreal.feature.chat.domain.TokenComposition
import co.ratmo.anreal.feature.chat.domain.TokenUsage
import co.ratmo.anreal.feature.chat.domain.UsageSummary
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class HealthDto(val ok: Boolean)

@Serializable
data class UsageSummaryDto(
    val storage: StorageUsageDto,
    val tokens: TokenUsageDto,
    val byModel: List<ModelUsageDto> = emptyList(),
    val byReasoningEffort: List<ReasoningUsageDto> = emptyList(),
)

@Serializable
data class StorageUsageDto(
    val usedBytes: Long,
    val maxBytes: Long,
    val remainingBytes: Long,
)

@Serializable
data class TokenUsageDto(
    val requestCount: Long,
    val inputTokens: Long,
    val outputTokens: Long,
    val totalTokens: Long,
    val cachedInputTokens: Long,
    val cacheCreationInputTokens: Long,
    val composition: TokenCompositionDto,
)

@Serializable
data class TokenCompositionDto(
    val inputUncached: Long,
    val cacheRead: Long,
    val output: Long,
)

@Serializable
data class ModelUsageDto(
    val model: String,
    val requestCount: Long,
    val totalTokens: Long,
)

@Serializable
data class ReasoningUsageDto(
    val reasoningEffort: String,
    val requestCount: Long,
    val totalTokens: Long,
)

@Serializable
data class ProfilingSettingsDto(
    val user: ProfileDto? = null,
    val projects: List<ProjectProfileDto> = emptyList(),
)

@Serializable
data class ProjectProfileDto(
    val id: String,
    val name: String,
    val profile: ProfileDto? = null,
)

@Serializable
data class ProfileDto(
    val sections: JsonObject = JsonObject(emptyMap()),
    val explicitFacts: List<ExplicitFactDto> = emptyList(),
    val updatedAt: String = "",
)

@Serializable
data class ExplicitFactDto(
    val section: String? = null,
    val fact: String,
    val createdAt: String,
)

fun UsageSummaryDto.toUsageSummary(): UsageSummary = UsageSummary(
    storage = StorageUsage(storage.usedBytes, storage.maxBytes, storage.remainingBytes),
    tokens = TokenUsage(
        requestCount = tokens.requestCount,
        inputTokens = tokens.inputTokens,
        outputTokens = tokens.outputTokens,
        totalTokens = tokens.totalTokens,
        cachedInputTokens = tokens.cachedInputTokens,
        cacheCreationInputTokens = tokens.cacheCreationInputTokens,
        composition = TokenComposition(
            inputUncached = tokens.composition.inputUncached,
            cacheRead = tokens.composition.cacheRead,
            output = tokens.composition.output,
        ),
    ),
    byModel = byModel.map { ModelUsage(it.model, it.requestCount, it.totalTokens) },
    byReasoningEffort = byReasoningEffort.map {
        ReasoningUsage(it.reasoningEffort, it.requestCount, it.totalTokens)
    },
)

fun ProfilingSettingsDto.toPersonalizationSettings(): PersonalizationSettings =
    PersonalizationSettings(
        user = user?.toProfile(),
        projects = projects.map { ProjectPersonalization(it.id, it.name, it.profile?.toProfile()) },
    )

private fun ProfileDto.toProfile(): PersonalizationProfile = PersonalizationProfile(
    sections = PROFILE_SECTION_KEYS.associateWith { key -> sections[key].toProfileBullets() },
    explicitFacts = explicitFacts.map {
        ExplicitProfileFact(section = it.section, fact = it.fact, createdAt = it.createdAt)
    },
    updatedAt = updatedAt,
)

private fun Any?.toProfileBullets(): List<ProfileBullet> {
    val items = this as? JsonArray ?: return emptyList()
    return items.mapNotNull { item ->
        when (item) {
            is JsonPrimitive -> item.contentOrNull?.takeIf(String::isNotBlank)?.let {
                ProfileBullet(text = it, sourceSessionIds = emptyList())
            }
            is JsonObject -> {
                val text = (item["text"] as? JsonPrimitive)?.contentOrNull
                    ?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val sources = (item["sources"] as? JsonArray).orEmpty().mapNotNull { source ->
                    (source as? JsonPrimitive)?.contentOrNull
                }
                ProfileBullet(text = text, sourceSessionIds = sources)
            }
            else -> null
        }
    }
}

private val PROFILE_SECTION_KEYS = listOf("facts", "preferences", "interests", "expertise", "goals")
