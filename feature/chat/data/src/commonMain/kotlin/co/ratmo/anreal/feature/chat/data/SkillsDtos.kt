package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.data.network.delete
import co.ratmo.anreal.core.data.network.get
import co.ratmo.anreal.core.data.network.patch
import co.ratmo.anreal.core.data.network.post
import co.ratmo.anreal.core.data.network.put
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.domain.util.asEmptyResult
import co.ratmo.anreal.core.domain.util.map
import co.ratmo.anreal.feature.chat.domain.Skill
import co.ratmo.anreal.feature.chat.domain.SkillStatus
import co.ratmo.anreal.feature.chat.domain.SkillsRemoteDataSource
import io.ktor.client.HttpClient
import kotlinx.serialization.Serializable

@Serializable
data class SkillDto(
    val id: String,
    val userId: String = "",
    val name: String,
    val description: String = "",
    val bodyMd: String = "",
    val isEnabled: Boolean = true,
    val status: String = "active",
    val version: Int = 1,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class SkillBodyDto(val name: String, val description: String, val bodyMd: String)

@Serializable
data class SkillReviewDto(val name: String, val description: String, val bodyMd: String, val markReviewed: Boolean = false)

@Serializable
data class SkillEnabledDto(val isEnabled: Boolean)

fun SkillDto.toSkill(): Skill = Skill(
    id = id, name = name, description = description, bodyMd = bodyMd,
    isEnabled = isEnabled, status = when (status) {
        "draft" -> SkillStatus.Draft
        "invalid" -> SkillStatus.Invalid
        else -> SkillStatus.Active
    }, version = version,
)

class KtorSkillsDataSource(private val httpClient: HttpClient) : SkillsRemoteDataSource {
    override suspend fun listSkills(): Result<List<Skill>, DataError.Network> =
        httpClient.get<List<SkillDto>>(route = "/api/skills").map { dtos -> dtos.map { it.toSkill() } }

    override suspend fun getSkill(id: String): Result<Skill, DataError.Network> =
        httpClient.get<SkillDto>(route = "/api/skills/$id").map { it.toSkill() }

    override suspend fun createSkill(name: String, description: String, bodyMd: String): Result<Skill, DataError.Network> =
        httpClient.post<SkillBodyDto, SkillDto>(route = "/api/skills", body = SkillBodyDto(name, description, bodyMd)).map { it.toSkill() }

    override suspend fun updateSkill(id: String, name: String, description: String, bodyMd: String, markReviewed: Boolean): Result<Skill, DataError.Network> =
        httpClient.put<SkillReviewDto, SkillDto>(route = "/api/skills/$id", body = SkillReviewDto(name, description, bodyMd, markReviewed)).map { it.toSkill() }

    override suspend fun setSkillEnabled(id: String, isEnabled: Boolean): Result<Skill, DataError.Network> =
        httpClient.patch<SkillEnabledDto, SkillDto>(route = "/api/skills/$id/enabled", body = SkillEnabledDto(isEnabled)).map { it.toSkill() }

    override suspend fun deleteSkill(id: String): Result<Unit, DataError.Network> =
        httpClient.delete(route = "/api/skills/$id").asEmptyResult()
}
