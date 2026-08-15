package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.component.GlassDrawer
import co.ratmo.anreal.core.designsystem.component.glassHighlightColor
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.presentation.ChatAction
import co.ratmo.anreal.feature.chat.presentation.ChatState
import co.ratmo.anreal.feature.chat.presentation.CitedDocumentUi
import co.ratmo.anreal.feature.chat.presentation.SessionDocumentUi
import co.ratmo.anreal.feature.chat.presentation.preview.chatDocumentsPreviewState
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Close
import com.composables.icons.materialsymbols.rounded.Description
import com.composables.icons.materialsymbols.rounded.Expand_less
import com.composables.icons.materialsymbols.rounded.Expand_more
import com.composables.icons.materialsymbols.rounded.Format_quote

@Composable
internal fun DocumentsEndDrawer(
    open: Boolean,
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DrawerDefaults.scrimColor)
                    .clickable(onClick = onDismiss),
            )
        }
        AnimatedVisibility(
            visible = open,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
        ) {
            GlassDrawer(fromEnd = true) {
                DocumentsDrawer(state = state, onAction = onAction)
            }
        }
    }
}

@Composable
internal fun DocumentsDrawer(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
) {
    val active = state.activeDocuments
    val cited = state.citedDocuments
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(
                horizontal = AnrealSpacing.md,
                vertical = AnrealSpacing.sm,
            ),
        ) {
            Text(
                text = AnrealCopy.get(AnrealCopy.LABEL_DOCUMENTS),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = documentsSummary(active.size, cited.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AnrealSpacing.sm, vertical = AnrealSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
        ) {
            if (active.isNotEmpty()) {
                DocumentSection(title = AnrealCopy.get(AnrealCopy.LABEL_ACTIVE_DOCUMENTS)) {
                    active.forEach { document ->
                        ActiveDocumentCard(
                            document = document,
                            onRemove = { onAction(ChatAction.OnRemoveActiveDocument(document.id)) },
                        )
                    }
                }
            }
            if (cited.isNotEmpty()) {
                DocumentSection(title = AnrealCopy.get(AnrealCopy.LABEL_CITED_DOCUMENTS)) {
                    cited.forEach { document ->
                        CitedDocumentCard(document = document)
                    }
                }
            }
            if (active.isEmpty() && cited.isEmpty()) {
                Text(
                    text = AnrealCopy.get(AnrealCopy.DOCUMENTS_EMPTY_BODY),
                    modifier = Modifier.padding(AnrealSpacing.md),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DocumentSection(
    title: String,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = AnrealSpacing.xxs, vertical = AnrealSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = if (expanded) {
                    MaterialSymbols.Rounded.Expand_less
                } else {
                    MaterialSymbols.Rounded.Expand_more
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) content()
    }
}

@Composable
private fun ActiveDocumentCard(
    document: SessionDocumentUi,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = glassHighlightColor(),
    ) {
        Column(modifier = Modifier.padding(AnrealSpacing.sm)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Description,
                    contentDescription = null,
                    modifier = Modifier.padding(top = 2.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = document.filename,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = AnrealSpacing.xs),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Close,
                        contentDescription = AnrealCopy.get(AnrealCopy.CD_REMOVE_DOCUMENT),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = document.summary.ifBlank {
                    AnrealCopy.get(AnrealCopy.DOCUMENTS_EMPTY_BODY)
                },
                modifier = Modifier.padding(top = AnrealSpacing.xs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CitedDocumentCard(
    document: CitedDocumentUi,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = glassHighlightColor(),
    ) {
        Row(
            modifier = Modifier.padding(AnrealSpacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Format_quote,
                contentDescription = null,
                modifier = Modifier.padding(top = 2.dp).size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = AnrealSpacing.xs),
            ) {
                Text(
                    text = document.filename,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = UiText.StringResource(
                        AnrealCopy.LABEL_CITATION_COUNT,
                        listOf(document.citationCount.toString()),
                    ).asString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun documentsSummary(activeCount: Int, citedCount: Int): String {
    if (activeCount == 0 && citedCount == 0) {
        return AnrealCopy.get(AnrealCopy.LABEL_DOCUMENTS_NONE)
    }
    return UiText.StringResource(
        AnrealCopy.LABEL_DOCUMENTS_SUMMARY,
        listOf(activeCount.toString(), citedCount.toString()),
    ).asString()
}

internal fun documentsBadgeCount(state: ChatState): Int {
    return state.activeDocuments.size + state.citedDocuments.size
}

@AnrealPreviews
@Composable
private fun DocumentsDrawerPopulatedPreview() {
    AnrealPreview {
        DocumentsDrawer(state = chatDocumentsPreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun DocumentsDrawerEmptyPreview() {
    AnrealPreview {
        DocumentsDrawer(state = ChatState(sessionsLoading = false), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun DocumentsEndDrawerOpenPreview() {
    AnrealPreview {
        Box(modifier = Modifier.fillMaxSize()) {
            DocumentsEndDrawer(
                open = true,
                state = chatDocumentsPreviewState(),
                onAction = {},
                onDismiss = {},
            )
        }
    }
}
