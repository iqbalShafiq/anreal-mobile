package co.ratmo.anreal.feature.chat.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.Json
import kotlin.test.Test

class CapabilitiesMetadataTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun capabilities_old_payload_still_parses_with_zero_counts() {
        val dto = json.decodeFromString<CapabilitiesDto>(
            """{"webSearchAvailable":true,"deepResearchAvailable":false,"imageGenerationAvailable":true,"context7Configured":false}""",
        )
        val capabilities = dto.toCapabilities()
        assertThat(capabilities.userSkillsCount).isEqualTo(0)
        assertThat(capabilities.userMcpCount).isEqualTo(0)
    }

    @Test
    fun capabilities_new_counts_map() {
        val dto = json.decodeFromString<CapabilitiesDto>(
            """{"webSearchAvailable":false,"deepResearchAvailable":false,"imageGenerationAvailable":false,"context7Configured":false,"userSkillsCount":3,"userMcpCount":1,"futureFlag":true}""",
        )
        val capabilities = dto.toCapabilities()
        assertThat(capabilities.userSkillsCount).isEqualTo(3)
        assertThat(capabilities.userMcpCount).isEqualTo(1)
    }
}
