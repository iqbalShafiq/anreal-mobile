package co.ratmo.anreal.feature.chat.presentation

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.feature.chat.presentation.component.formatThoughtDuration
import co.ratmo.anreal.feature.chat.presentation.component.formatToolInput
import co.ratmo.anreal.feature.chat.presentation.component.formatToolOutput
import co.ratmo.anreal.feature.chat.presentation.component.toolActivityLabel
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class ToolActivityFormatTest {

    @Test
    fun thought_duration_matches_web_thresholds() {
        assertThat(formatThoughtDuration(400.milliseconds)).isEqualTo("0.4s")
        assertThat(formatThoughtDuration(2_300.milliseconds)).isEqualTo("2.3s")
        assertThat(formatThoughtDuration(12_400.milliseconds)).isEqualTo("12s")
    }

    @Test
    fun web_search_request_and_results_are_formatted() {
        val request = formatToolInput("web_search", """{"query":"latest Kotlin","maxResults":5}""")
        assertThat(request.fields).contains("Query" to "latest Kotlin")
        assertThat(request.fields).contains("Max results" to "5")

        val result = formatToolOutput(
            toolName = "web_search",
            raw = """{"results":[{"title":"Kotlin 2.2","url":"https://kotlinlang.org","content":"New release"}]}""",
            errorMessage = null,
            running = false,
        )
        assertThat(result.summary).isEqualTo("1 result")
        assertThat(result.items.single().title).isEqualTo("Kotlin 2.2")
    }

    @Test
    fun running_tool_shows_working_placeholder() {
        val result = formatToolOutput("web_search", raw = null, errorMessage = null, running = true)
        assertThat(result.emptyText).isEqualTo(AnrealCopy.get(AnrealCopy.LABEL_TOOL_WORKING))
    }

    @Test
    fun known_tool_names_use_activity_labels() {
        assertThat(toolActivityLabel("web_search")).isEqualTo("Searching the web")
        assertThat(toolActivityLabel("mystery")).isNotNull()
    }
}
