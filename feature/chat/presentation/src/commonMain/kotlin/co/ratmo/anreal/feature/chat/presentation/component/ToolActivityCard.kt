package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.component.AnrealLoadingIndicator
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealMotion
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.designsystem.theme.LocalAnrealReduceMotion
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Expand_more

@Composable
internal fun ToolActivityCard(
    part: ChatPart.Tool,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean? = null,
) {
    val running = part.state == "input-streaming" || part.state == "input-available"
    val error = part.state == "error"
    val done = part.state == "output-available"
    var expanded by remember(part.id) { mutableStateOf(initiallyExpanded ?: (running || error)) }
    var userToggled by remember(part.id) { mutableStateOf(false) }
    var previousState by remember(part.id) { mutableStateOf(part.state) }

    LaunchedEffect(part.state) {
        val wasRunning = previousState == "input-streaming" || previousState == "input-available"
        previousState = part.state
        if (userToggled) return@LaunchedEffect
        expanded = when {
            wasRunning && done -> false
            running || error -> true
            else -> expanded
        }
    }

    val status = when {
        error -> AnrealCopy.get(AnrealCopy.TOOL_STATUS_ERROR)
        done -> AnrealCopy.get(AnrealCopy.TOOL_STATUS_DONE)
        else -> AnrealCopy.get(AnrealCopy.TOOL_STATUS_WORKING)
    }
    val labelColor = when {
        error -> MaterialTheme.colorScheme.error
        running -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusColor = when {
        error -> MaterialTheme.colorScheme.error
        running -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val request = formatToolInput(part.toolName, part.input)
    val result = formatToolOutput(part.toolName, part.output, part.errorMessage, running)

    Column(modifier = modifier.fillMaxWidth()) {
        ActivityToggleRow(
            expanded = expanded,
            running = running,
            label = toolActivityLabel(part.toolName),
            status = status,
            labelColor = labelColor,
            statusColor = statusColor,
            contentDescription = AnrealCopy.get(AnrealCopy.CD_TOGGLE_TOOL),
            onClick = {
                userToggled = true
                expanded = !expanded
            },
        )
        if (expanded) {
            ActivityDetails(
                error = error,
                sections = listOf(request, result),
            )
        }
    }
}

@Composable
internal fun ActivityToggleRow(
    expanded: Boolean,
    running: Boolean,
    label: String,
    status: String?,
    labelColor: Color,
    statusColor: Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val reduceMotion = LocalAnrealReduceMotion.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = AnrealMotion.selectionSpec(),
        label = "activity-chevron",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AnrealSpacing.touch)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
    ) {
        Icon(
            imageVector = MaterialSymbols.Rounded.Expand_more,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.graphicsLayer {
                rotationZ = if (reduceMotion) {
                    if (expanded) 0f else -90f
                } else {
                    rotation
                }
            },
        )
        if (running) {
            AnrealLoadingIndicator(size = 14.dp)
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
            maxLines = 1,
        )
        if (status != null) {
            Text(
                text = "· $status",
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
            )
        }
    }
}

@Composable
private fun ActivityDetails(
    error: Boolean,
    sections: List<ToolSectionUi>,
) {
    val lineColor = if (error) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Column(
        modifier = Modifier
            .padding(start = AnrealSpacing.lg, top = AnrealSpacing.xxs, bottom = AnrealSpacing.xs)
            .drawBehind {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(start = AnrealSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
    ) {
        sections.forEach { section ->
            ToolSectionView(section)
        }
    }
}

@Composable
private fun ToolSectionView(section: ToolSectionUi) {
    Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs)) {
        Text(
            text = section.title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        section.summary?.let { summary ->
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        section.fields.forEach { (label, value) ->
            Row(horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        section.items.forEach { item ->
            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                item.meta?.let { meta ->
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item.detail?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (section.summary == null && section.fields.isEmpty() && section.items.isEmpty()) {
            section.emptyText?.let { empty ->
                Text(
                    text = empty,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        "pearson_correlation" -> AnrealCopy.TOOL_PEARSON
        "linear_regression" -> AnrealCopy.TOOL_LINEAR_REGRESSION
        "get_document_page_images" -> AnrealCopy.TOOL_PAGE_IMAGES
        "request_clarification" -> AnrealCopy.TOOL_CLARIFICATION
        "resolve-library-id" -> AnrealCopy.TOOL_RESOLVE_LIBRARY
        "query-docs" -> AnrealCopy.TOOL_QUERY_DOCS
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
            part = ChatPart.Tool(
                id = "t1",
                toolName = "web_search",
                toolCallId = "c1",
                state = "output-available",
                input = """{"query":"latest Kotlin version"}""",
                output = """{"results":[{"title":"Kotlin 2.2","url":"https://kotlinlang.org","content":"Released this year."}]}""",
            ),
            initiallyExpanded = true,
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
