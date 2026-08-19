package co.ratmo.anreal.feature.chat.presentation.component

import co.ratmo.anreal.core.presentation.AnrealCopy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

internal data class ToolSectionUi(
    val title: String,
    val summary: String? = null,
    val fields: List<Pair<String, String>> = emptyList(),
    val items: List<ToolItemUi> = emptyList(),
    val emptyText: String? = null,
)

internal data class ToolItemUi(
    val title: String,
    val meta: String? = null,
    val detail: String? = null,
)

private val toolJson = Json { ignoreUnknownKeys = true }

internal fun parseToolValue(raw: String?): JsonElement? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    return runCatching { toolJson.parseToJsonElement(trimmed) }.getOrNull()
}

internal fun formatToolInput(toolName: String, raw: String?): ToolSectionUi {
    val value = parseToolValue(raw)
    val request = AnrealCopy.get(AnrealCopy.LABEL_TOOL_REQUEST)
    val waiting = AnrealCopy.get(AnrealCopy.LABEL_TOOL_WAITING_REQUEST)
    if (value == null) {
        return ToolSectionUi(title = request, emptyText = waiting)
    }
    val obj = value as? JsonObject
    val fields = when (toolName) {
        "web_search", "find_documents", "search_document_pages" -> listOfNotNull(
            obj.field("query", "Query", 400),
            obj.field("reason", "Reason", 320),
            obj.countField("documentIds", "Documents", "document"),
            obj.numberField("limit", "Limit"),
            obj.numberField("maxResults", "Max results"),
        )
        "web_fetch" -> listOfNotNull(
            obj.field("url", "URL", 320),
            obj.field("reason", "Reason", 320),
        )
        "generate_image", "edit_image" -> listOfNotNull(
            obj.field("prompt", "Prompt", 400),
            obj.field("modelId", "Model", 80),
            obj.field("aspectRatio", "Aspect ratio", 40),
        )
        else -> genericFields(value)
    }
    return ToolSectionUi(
        title = request,
        fields = fields,
        emptyText = if (fields.isEmpty()) waiting else null,
    )
}

internal fun formatToolOutput(
    toolName: String,
    raw: String?,
    errorMessage: String?,
    running: Boolean,
): ToolSectionUi {
    val result = AnrealCopy.get(AnrealCopy.LABEL_TOOL_RESULT)
    if (errorMessage != null) {
        return ToolSectionUi(
            title = AnrealCopy.get(AnrealCopy.TOOL_STATUS_ERROR),
            fields = listOf("Message" to errorMessage),
        )
    }
    if (running) {
        return ToolSectionUi(
            title = result,
            emptyText = AnrealCopy.get(AnrealCopy.LABEL_TOOL_WORKING),
        )
    }
    val value = parseToolValue(raw)
    if (value == null) {
        return ToolSectionUi(title = result, emptyText = "No result yet")
    }
    val obj = value as? JsonObject
    return when (toolName) {
        "web_search" -> formatListedOutput(
            title = result,
            items = obj.array("results").mapNotNull { element ->
                val item = element as? JsonObject ?: return@mapNotNull null
                ToolItemUi(
                    title = item.string("title") ?: item.string("url") ?: "Result",
                    meta = item.string("url")?.hostname(),
                    detail = item.string("content")?.truncate(220),
                )
            },
            empty = "No web results returned.",
            noun = "result",
        )
        "find_documents" -> formatListedOutput(
            title = result,
            items = obj.array("results").mapNotNull { element ->
                val item = element as? JsonObject ?: return@mapNotNull null
                ToolItemUi(
                    title = item.string("filename") ?: "Document",
                    meta = item.int("pageCount")?.let { "$it pages" },
                    detail = (item.string("firstPageSummary") ?: item.string("summary"))?.truncate(200),
                )
            },
            empty = "No documents matched this query.",
            noun = "document",
        )
        "web_fetch" -> ToolSectionUi(
            title = result,
            summary = if (obj.string("title") != null) "Page fetched" else "Not fetched",
            fields = listOfNotNull(
                obj.field("title", "Title", 200),
                obj.field("url", "URL", 200),
                obj.field("content", "Content", 400),
            ),
        )
        else -> ToolSectionUi(
            title = result,
            fields = genericFields(value),
            emptyText = if (genericFields(value).isEmpty()) "No result details" else null,
        )
    }
}

private fun formatListedOutput(
    title: String,
    items: List<ToolItemUi>,
    empty: String,
    noun: String,
): ToolSectionUi {
    return ToolSectionUi(
        title = title,
        summary = if (items.isEmpty()) empty else "${items.size} ${if (items.size == 1) noun else "${noun}s"}",
        items = items.take(6),
        emptyText = if (items.isEmpty()) empty else null,
    )
}

private fun genericFields(value: JsonElement, maxFields: Int = 8): List<Pair<String, String>> {
    val obj = value as? JsonObject ?: return value.asDisplayString()?.let { listOf("Value" to it.truncate(400)) }.orEmpty()
    return obj.entries.asSequence()
        .filterNot { it.value is JsonNull }
        .take(maxFields)
        .mapNotNull { (key, entry) ->
            val display = when (entry) {
                is JsonPrimitive -> entry.contentOrNull?.truncate(320)
                is JsonArray -> "${entry.size} items"
                is JsonObject -> "${entry.size} fields"
                is JsonNull -> null
            } ?: return@mapNotNull null
            humanizeKey(key) to display
        }
        .toList()
}

private fun JsonObject?.field(key: String, label: String, max: Int): Pair<String, String>? {
    val text = this?.string(key)?.truncate(max) ?: return null
    return label to text
}

private fun JsonObject?.numberField(key: String, label: String): Pair<String, String>? {
    val number = this?.get(key)?.jsonPrimitive?.doubleOrNull ?: return null
    return label to if (number % 1.0 == 0.0) number.toInt().toString() else number.toString()
}

private fun JsonObject?.countField(key: String, label: String, noun: String): Pair<String, String>? {
    val count = this?.array(key)?.size ?: return null
    if (count == 0) return null
    return label to "$count ${if (count == 1) noun else "${noun}s"}"
}

private fun JsonObject?.string(key: String): String? = this?.get(key)?.jsonPrimitive?.contentOrNull

private fun JsonObject?.int(key: String): Int? = this?.get(key)?.jsonPrimitive?.doubleOrNull?.toInt()

private fun JsonObject?.array(key: String): JsonArray {
    return runCatching { this?.get(key)?.jsonArray }.getOrNull() ?: JsonArray(emptyList())
}

private fun JsonElement.asDisplayString(): String? {
    val primitive = this as? JsonPrimitive ?: return toString()
    return primitive.contentOrNull ?: if (primitive.booleanOrNull != null) primitive.toString() else null
}

private fun humanizeKey(key: String): String {
    return key
        .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
        .replace(Regex("[_-]+"), " ")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

internal fun String.truncate(max: Int): String {
    val normalized = replace(Regex("\\s+"), " ").trim()
    if (normalized.length <= max) return normalized
    return normalized.take(max - 1).trimEnd() + "…"
}

private fun String.hostname(): String? {
    val host = removePrefix("https://").removePrefix("http://").substringBefore("/")
    return host.ifBlank { null }
}
