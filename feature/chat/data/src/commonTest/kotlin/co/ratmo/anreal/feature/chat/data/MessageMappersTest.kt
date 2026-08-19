package co.ratmo.anreal.feature.chat.data

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.database.MessageEntity
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

class MessageMappersTest {

    @Test
    fun entity_round_trips_text_reasoning_and_tool() {
        val message = ChatMessage(
            id = "m1",
            role = ChatRole.Assistant,
            parts = listOf(
                ChatPart.Reasoning(id = "r1", text = "plan"),
                ChatPart.Tool(
                    id = "t1",
                    toolName = "find_documents",
                    toolCallId = "c1",
                    state = "output-available",
                ),
                ChatPart.Text(id = "p1", text = "Revenue grew."),
            ),
            isComplete = true,
        )

        val restored = message.toEntity(sessionId = "s1", position = 0).toMessage()
        assertThat(restored).isEqualTo(message)
    }

    @Test
    fun legacy_entity_without_parts_json_uses_text() {
        val entity = MessageEntity(
            sessionId = "s1",
            id = "m1",
            role = "user",
            text = "Hello",
            isComplete = true,
            position = 0,
            partsJson = "",
        )
        assertThat(entity.toMessage()).isEqualTo(
            ChatMessage(
                id = "m1",
                role = ChatRole.User,
                parts = listOf(ChatPart.Text(id = "m1-text", text = "Hello")),
                isComplete = true,
            ),
        )
    }

    @Test
    fun history_dto_maps_text_only() {
        val dto = historyMessageDto(
            role = "user",
            parts = listOf(HistoryContentDto(type = "text", text = "Hi")),
        )
        assertThat(dto.toMessage(0).parts).containsExactly(
            ChatPart.Text(id = "history-0-0", text = "Hi"),
        )
    }

    @Test
    fun history_dto_maps_system_string_content() {
        val dto = HistoryMessageDto(
            role = "system",
            content = JsonPrimitive("Earlier turns were summarized."),
            metadata = HistoryMetadataDto(),
        )
        val message = dto.toMessage(1)
        assertThat(message.role).isEqualTo(ChatRole.System)
        assertThat(message.parts).containsExactly(
            ChatPart.Text(id = "history-1-0", text = "Earlier turns were summarized."),
        )
    }

    @Test
    fun history_merges_tool_results_into_assistant_tool_call() {
        val assistant = historyMessageDto(
            role = "assistant",
            parts = listOf(
                HistoryContentDto(type = "text", text = "Let me look that up."),
            ),
        ).toMessage(0).copy(
            parts = listOf(
                ChatPart.Tool(
                    id = "call-1",
                    toolName = "web_search",
                    toolCallId = "call-1",
                    state = "input-available",
                    input = """{"query":"kotlin"}""",
                ),
            ),
        )
        val tool = HistoryMessageDto(
            role = "tool",
            content = Json.parseToJsonElement(
                """[{"type":"tool_result","id":"call-1","toolName":"web_search","content":[{"type":"text","text":"ok"}]}]""",
            ),
        ).toMessage(1)
        val merged = listOf(assistant, tool).mergeToolResultMessages()
        assertThat(merged.size).isEqualTo(1)
        val part = merged.single().parts.single() as ChatPart.Tool
        assertThat(part.state).isEqualTo("output-available")
        assertThat(part.output != null).isEqualTo(true)
    }

    @Test
    fun catalog_filters_image_models() {
        val catalog = ModelCatalogDto(
            models = listOf(
                ModelInfoDto(modelId = "chat-1", label = "DeepSeek", outputType = "text"),
                ModelInfoDto(modelId = "img-1", label = "Flux", outputType = "image"),
            ),
            reasoningEfforts = listOf(ReasoningEffortDto(key = "high", label = "High")),
        ).toCatalog()
        assertThat(catalog.models.map { it.id }).isEqualTo(listOf("chat-1"))
        assertThat(catalog.efforts.single().key).isEqualTo("high")
    }

    @Test
    fun history_dto_maps_tool_content() {
        val dto = historyMessageDto(
            role = "assistant",
            parts = listOf(
                HistoryContentDto(
                    type = "tool",
                    id = "t1",
                    toolName = "web_search",
                    toolCallId = "c1",
                    state = "output-available",
                ),
                HistoryContentDto(type = "text", id = "p1", text = "Done."),
            ),
        )
        assertThat(dto.toMessage(2).parts).containsExactly(
            ChatPart.Tool(
                id = "t1",
                toolName = "web_search",
                toolCallId = "c1",
                state = "output-available",
            ),
            ChatPart.Text(id = "p1", text = "Done."),
        )
    }
}
