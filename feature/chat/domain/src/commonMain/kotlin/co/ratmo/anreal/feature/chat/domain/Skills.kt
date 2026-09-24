package co.ratmo.anreal.feature.chat.domain

import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result

enum class SkillStatus { Active, Invalid, Draft }

data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val bodyMd: String = "",
    val isEnabled: Boolean = true,
    val status: SkillStatus = SkillStatus.Active,
    val version: Int = 1,
)

interface SkillsRemoteDataSource {
    suspend fun listSkills(): Result<List<Skill>, DataError.Network>
    suspend fun getSkill(id: String): Result<Skill, DataError.Network>
    suspend fun createSkill(name: String, description: String, bodyMd: String): Result<Skill, DataError.Network>
    suspend fun updateSkill(id: String, name: String, description: String, bodyMd: String, markReviewed: Boolean): Result<Skill, DataError.Network>
    suspend fun setSkillEnabled(id: String, isEnabled: Boolean): Result<Skill, DataError.Network>
    suspend fun deleteSkill(id: String): Result<Unit, DataError.Network>
}

val SKILL_NAME_RE = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")

fun validateSkillInput(name: String, description: String, bodyMd: String): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    val trimmedName = name.trim()
    if (trimmedName.isEmpty() || trimmedName.length > 64 || !SKILL_NAME_RE.matches(trimmedName)) {
        errors["name"] = "Lowercase letters, numbers, hyphens."
    }
    if (description.trim().isEmpty() || description.trim().length > 1024) {
        errors["description"] = "One line the agent uses to decide fit."
    }
    val body = bodyMd.trim()
    if (body.isEmpty() || body.length > 16000) errors["bodyMd"] = "Starts with --- frontmatter."
    else {
        val fence = extractFrontmatter(body)
        if (fence == null) errors["bodyMd"] = "Starts with --- frontmatter."
        else {
            if (unquote(fence["name"]) != trimmedName) errors["bodyMd"] = "Frontmatter name must equal the skill name."
            else if (unquote(fence["description"]) != description.trim()) errors["bodyMd"] = "Frontmatter description must match."
        }
    }
    return errors
}

private fun extractFrontmatter(body: String): Map<String, String>? {
    val lines = body.lines()
    if (lines.firstOrNull()?.trim() != "---") return null
    val end = lines.drop(1).indexOfFirst { it.trim() == "---" }
    if (end < 0) return null
    return lines.drop(1).take(end).mapNotNull { line ->
        val key = line.substringBefore(":").trim()
        val value = line.substringAfter(":", "").trim()
        if (key.isEmpty() || value.isEmpty()) null else key to value
    }.toMap()
}

private fun unquote(value: String?): String? {
    if (value == null) return null
    return value.removeSurrounding("\"").removeSurrounding("'")
}
