package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart

@Composable
internal fun ToolActivityCard(
    part: ChatPart.Tool,
    modifier: Modifier = Modifier,
) {
    val working = part.state != "output-available" && part.state != "error"
    val status = when (part.state) {
        "output-available" -> AnrealCopy.get(AnrealCopy.TOOL_STATUS_DONE)
        "error" -> AnrealCopy.get(AnrealCopy.TOOL_STATUS_ERROR)
        else -> AnrealCopy.get(AnrealCopy.TOOL_STATUS_WORKING)
    }
    val statusColor = if (part.state == "error") {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(AnrealSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
        ) {
            if (working) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
            }
            Column {
                Text(
                    text = toolActivityLabel(part.toolName),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                )
            }
        }
    }
}

internal fun toolActivityLabel(toolName: String): String {
    val key = when (toolName) {
        "find_documents" -> AnrealCopy.TOOL_FIND_DOCUMENTS
        "search_document_pages" -> AnrealCopy.TOOL_SEARCH_DOCUMENT_PAGES
        "get_document_next_page" -> AnrealCopy.TOOL_GET_DOCUMENT_NEXT_PAGE
        "web_search" -> AnrealCopy.TOOL_WEB_SEARCH
        "web_fetch" -> AnrealCopy.TOOL_WEB_FETCH
        "generate_image" -> AnrealCopy.TOOL_GENERATE_IMAGE
        "edit_image" -> AnrealCopy.TOOL_EDIT_IMAGE
        "view_image" -> AnrealCopy.TOOL_VIEW_IMAGE
        "descriptive_stats" -> AnrealCopy.TOOL_DESCRIPTIVE_STATS
        else -> null
    }
    return if (key != null) {
        AnrealCopy.get(key)
    } else {
        UiText.StringResource(AnrealCopy.TOOL_RUNNING_FALLBACK, listOf(toolName)).asString()
    }
}

@AnrealPreviews
@Composable
private fun ToolActivityCardWorkingPreview() {
    AnrealPreview {
        ToolActivityCard(
            ChatPart.Tool(
                id = "t1",
                toolName = "find_documents",
                toolCallId = "c1",
                state = "input-streaming",
            ),
        )
    }
}

@AnrealPreviews
@Composable
private fun ToolActivityCardDonePreview() {
    AnrealPreview {
        ToolActivityCard(
            ChatPart.Tool(
                id = "t1",
                toolName = "web_search",
                toolCallId = "c1",
                state = "output-available",
            ),
        )
    }
}

@AnrealPreviews
@Composable
private fun ToolActivityCardErrorPreview() {
    AnrealPreview {
        ToolActivityCard(
            ChatPart.Tool(
                id = "t1",
                toolName = "web_fetch",
                toolCallId = "c1",
                state = "error",
            ),
        )
    }
}

@AnrealPreviews
@Composable
private fun ToolActivityCardUnknownPreview() {
    AnrealPreview {
        ToolActivityCard(
            ChatPart.Tool(
                id = "t1",
                toolName = "custom_tool",
                toolCallId = "c1",
                state = "input-available",
            ),
        )
    }
}
