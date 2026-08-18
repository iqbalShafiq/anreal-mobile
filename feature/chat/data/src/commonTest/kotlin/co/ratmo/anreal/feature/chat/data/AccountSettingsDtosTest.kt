package co.ratmo.anreal.feature.chat.data

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.Json
import kotlin.test.Test

class AccountSettingsDtosTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun profile_mapper_accepts_current_object_shape_and_legacy_strings() {
        val dto = json.decodeFromString<ProfilingSettingsDto>(
            """{"user":{"sections":{"facts":[{"text":"Uses Kotlin","sources":["s1"]},"Likes concise answers"],"preferences":[]},"explicitFacts":[],"updatedAt":"now"},"projects":[]}""",
        )

        val profile = dto.toPersonalizationSettings().user ?: error("profile expected")

        assertThat(profile.sections.getValue("facts").map { it.text })
            .containsExactly("Uses Kotlin", "Likes concise answers")
        assertThat(profile.sections.getValue("facts").first().sourceSessionIds)
            .containsExactly("s1")
    }

    @Test
    fun usage_mapper_keeps_long_token_counts() {
        val dto = json.decodeFromString<UsageSummaryDto>(
            """{"storage":{"usedBytes":1,"maxBytes":2,"remainingBytes":1},"tokens":{"requestCount":3,"inputTokens":4000000000,"outputTokens":5,"totalTokens":4000000005,"cachedInputTokens":2,"cacheCreationInputTokens":1,"composition":{"inputUncached":3999999998,"cacheRead":2,"output":5}},"byModel":[],"byReasoningEffort":[]}""",
        )

        assertThat(dto.toUsageSummary().tokens.totalTokens).isEqualTo(4_000_000_005L)
    }
}
