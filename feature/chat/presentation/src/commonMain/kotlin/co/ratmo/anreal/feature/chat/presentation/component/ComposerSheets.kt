package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.component.AnrealBottomSheet
import co.ratmo.anreal.core.designsystem.component.AnrealLoadingIndicator
import co.ratmo.anreal.core.designsystem.component.AnrealSheetTitle
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.presentation.ChatAction
import co.ratmo.anreal.feature.chat.presentation.ChatState
import co.ratmo.anreal.feature.chat.domain.ChatModel
import co.ratmo.anreal.feature.chat.presentation.preview.chatComposerCatalogPreviewState
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Settings

internal enum class ComposerSheet {
    Model,
    Features,
    Attach,
}

@Composable
internal fun ComposerSheets(
    sheet: ComposerSheet?,
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    onOpenAttachments: () -> Unit = {},
    onManageSkills: () -> Unit = {},
    onManageMcp: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    if (sheet == null) return
    AnrealBottomSheet(onDismiss = onDismiss) {
        ComposerSheetBody(
            sheet = sheet,
            state = state,
            onAction = onAction,
            onOpenAttachments = onOpenAttachments,
            onManageSkills = onManageSkills,
            onManageMcp = onManageMcp,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ComposerSheetBody(
    sheet: ComposerSheet,
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    onOpenAttachments: () -> Unit,
    onManageSkills: () -> Unit,
    onManageMcp: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (sheet) {
        ComposerSheet.Model -> ModelAndReasoningSheet(state = state, onAction = onAction)
        ComposerSheet.Features -> FeaturesSheet(
            state = state,
            onAction = onAction,
            onOpenAttachments = onOpenAttachments,
            onManageSkills = onManageSkills,
            onManageMcp = onManageMcp,
        )
        ComposerSheet.Attach -> AttachSheet(onAction = onAction, onDismiss = onDismiss)
    }
}

@Composable
private fun SheetOption(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                this.selected = selected
                contentDescription = title
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = AnrealSpacing.md, vertical = AnrealSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun ModelAndReasoningSheet(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
) {
    val allowed = state.models.firstOrNull { it.id == state.selectedModelId }?.reasoningEfforts.orEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = AnrealSpacing.lg),
        horizontalAlignment = Alignment.Start,
    ) {
        when {
            state.catalogLoading && state.models.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(AnrealSpacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    AnrealLoadingIndicator()
                }
            }
            state.catalogError != null && state.models.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(AnrealSpacing.md),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = state.catalogError.asString(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = { onAction(ChatAction.OnRetryCatalog) }) {
                        Text(AnrealCopy.get(AnrealCopy.ACTION_RETRY))
                    }
                }
            }
            state.models.isEmpty() -> Text(
                text = AnrealCopy.get(AnrealCopy.MODELS_EMPTY),
                modifier = Modifier.padding(AnrealSpacing.md),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> {
                state.catalogError?.let { error ->
                    CachedCatalogError(
                        error = error.asString(),
                        onRetry = { onAction(ChatAction.OnRetryCatalog) },
                    )
                }
                AnrealSheetTitle(AnrealCopy.get(AnrealCopy.LABEL_MODEL))
                state.models.forEach { model ->
                    SheetOption(
                        title = model.label,
                        subtitle = model.describe(),
                        selected = model.id == state.selectedModelId,
                        onClick = { onAction(ChatAction.OnSelectModel(model.id)) },
                    )
                }
                AnrealSheetTitle(AnrealCopy.get(AnrealCopy.LABEL_REASONING))
                SheetOption(
                    title = AnrealCopy.get(AnrealCopy.LABEL_REASONING_NONE),
                    subtitle = null,
                    selected = state.selectedReasoning == null,
                    onClick = { onAction(ChatAction.OnSelectReasoning(null)) },
                )
                state.reasoningEfforts.filter { it.key in allowed }.forEach { effort ->
                    SheetOption(
                        title = effort.label,
                        subtitle = effort.description,
                        selected = state.selectedReasoning == effort.key,
                        onClick = { onAction(ChatAction.OnSelectReasoning(effort.key)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CachedCatalogError(
    error: String,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AnrealSpacing.md, vertical = AnrealSpacing.sm),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(start = AnrealSpacing.md, end = AnrealSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = error,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onRetry) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_RETRY))
            }
        }
    }
}

private fun ChatModel.describe(): String? {
    val parts = mutableListOf<String>()
    if (providerName.isNotBlank()) parts += providerName
    val hintText = hint?.trim()?.takeIf { it.isNotEmpty() }
    if (hintText != null) parts += hintText
    if (contextWindowTokens > 0) parts += "${contextWindowTokens / 1000}k context"
    prices?.let { prices ->
        val input = prices.input
        val output = prices.output
        if (input != null || output != null) {
            val inputText = input?.let { "$$it/M in" } ?: "in n/a"
            val outputText = output?.let { "$$it/M out" } ?: "out n/a"
            parts += "$inputText · $outputText"
        }
    }
    if (inputModalities.isNotEmpty()) parts += inputModalities.joinToString(", ")
    description?.trim()?.takeIf { it.isNotEmpty() }?.let { parts += it }
    if (outputType == "image") parts += "Image model"
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
private fun FeaturesSheet(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    onOpenAttachments: () -> Unit,
    onManageSkills: () -> Unit,
    onManageMcp: () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = AnrealSpacing.lg)) {
        AnrealSheetTitle(AnrealCopy.get(AnrealCopy.CD_FEATURES))
        SheetOption(
            title = AnrealCopy.get(AnrealCopy.LABEL_ATTACH),
            subtitle = AnrealCopy.get(AnrealCopy.ATTACH_LIBRARY_BODY),
            selected = false,
            enabled = !state.isUploading,
            onClick = onOpenAttachments,
        )
        SheetOption(
            title = AnrealCopy.get(AnrealCopy.LABEL_WEB_SEARCH),
            subtitle = if (state.capabilities.webSearchAvailable) {
                null
            } else {
                AnrealCopy.get(AnrealCopy.FEATURE_UNAVAILABLE)
            },
            selected = state.webSearchEnabled,
            enabled = state.capabilities.webSearchAvailable,
            onClick = { onAction(ChatAction.OnToggleWebSearch) },
            trailing = {
                Switch(
                    checked = state.webSearchEnabled,
                    onCheckedChange = { onAction(ChatAction.OnToggleWebSearch) },
                    enabled = state.capabilities.webSearchAvailable,
                )
            },
        )
        SheetOption(
            title = AnrealCopy.get(AnrealCopy.LABEL_IMAGE_GEN),
            subtitle = if (state.capabilities.imageGenerationAvailable) {
                null
            } else {
                AnrealCopy.get(AnrealCopy.FEATURE_UNAVAILABLE)
            },
            selected = state.imageGenerationEnabled,
            enabled = state.capabilities.imageGenerationAvailable,
            onClick = { onAction(ChatAction.OnToggleImageGeneration) },
            trailing = {
                Switch(
                    checked = state.imageGenerationEnabled,
                    onCheckedChange = { onAction(ChatAction.OnToggleImageGeneration) },
                    enabled = state.capabilities.imageGenerationAvailable,
                )
            },
        )
        SheetOption(
            title = AnrealCopy.get(AnrealCopy.LABEL_DEEP_RESEARCH),
            subtitle = if (state.capabilities.deepResearchAvailable) {
                null
            } else {
                AnrealCopy.get(AnrealCopy.FEATURE_UNAVAILABLE)
            },
            selected = state.deepResearchEnabled,
            enabled = state.capabilities.deepResearchAvailable,
            onClick = { onAction(ChatAction.OnToggleDeepResearch) },
            trailing = {
                Switch(
                    checked = state.deepResearchEnabled,
                    onCheckedChange = { onAction(ChatAction.OnToggleDeepResearch) },
                    enabled = state.capabilities.deepResearchAvailable,
                )
            },
        )
        if (!state.catalogLoading) {
            val skillsOn = state.selectedSkillIds.isNotEmpty()
            EnhancementRow(
                title = AnrealCopy.get(AnrealCopy.LABEL_SKILLS),
                subtitle = when {
                    state.capabilities.userSkillsCount == 0 -> {
                        AnrealCopy.get(AnrealCopy.SKILLS_EMPTY)
                    }
                    skillsOn -> AnrealCopy.get(AnrealCopy.SKILLS_ON)
                    else -> AnrealCopy.get(AnrealCopy.SKILLS_OFF)
                },
                checked = skillsOn,
                enabled = state.capabilities.userSkillsCount > 0,
                activeCount = state.selectedSkillIds.size,
                totalCount = state.capabilities.userSkillsCount,
                manageDescription = AnrealCopy.get(AnrealCopy.CD_MANAGE_SKILLS),
                onToggle = { on ->
                    // Turning back on from empty restores nothing here: the ChatViewModel
                    // only intersects the ids it is given. Re-enable from the manager,
                    // which owns the full catalog.
                    onAction(ChatAction.OnSkillsToggle(if (on) state.selectedSkillIds else emptyList()))
                },
                onManage = onManageSkills,
            )
            val mcpOn = state.selectedMcpServerIds.isNotEmpty()
            EnhancementRow(
                title = AnrealCopy.get(AnrealCopy.LABEL_MCP),
                subtitle = when {
                    state.capabilities.userMcpCount == 0 -> {
                        AnrealCopy.get(AnrealCopy.MCP_EMPTY)
                    }
                    mcpOn -> AnrealCopy.get(AnrealCopy.MCP_ON)
                    else -> AnrealCopy.get(AnrealCopy.MCP_OFF)
                },
                checked = mcpOn,
                enabled = state.capabilities.userMcpCount > 0,
                activeCount = state.selectedMcpServerIds.size,
                totalCount = state.capabilities.userMcpCount,
                manageDescription = AnrealCopy.get(AnrealCopy.CD_MANAGE_MCP),
                onToggle = { on ->
                    onAction(ChatAction.OnMcpToggle(if (on) state.selectedMcpServerIds else emptyList()))
                },
                onManage = onManageMcp,
            )
        }
    }
}

@Composable
private fun EnhancementRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    activeCount: Int,
    totalCount: Int,
    manageDescription: String,
    onToggle: (Boolean) -> Unit,
    onManage: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                selected = checked
                contentDescription = title
            }
            .clickable(enabled = enabled, onClick = { onToggle(!checked) })
            .padding(horizontal = AnrealSpacing.md, vertical = AnrealSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (checked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CountBadge(active = activeCount, total = totalCount)
        IconButton(
            onClick = onManage,
            modifier = Modifier.size(AnrealSpacing.touch),
        ) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Settings,
                contentDescription = manageDescription,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            enabled = enabled,
        )
    }
}

@Composable
private fun CountBadge(active: Int, total: Int) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = "$active/$total",
            modifier = Modifier.padding(
                horizontal = AnrealSpacing.xs,
                vertical = AnrealSpacing.xxs,
            ),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun AttachSheet(
    onAction: (ChatAction) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = AnrealSpacing.lg)) {
        AnrealSheetTitle(AnrealCopy.get(AnrealCopy.LABEL_ATTACH))
        SheetOption(
            title = AnrealCopy.get(AnrealCopy.LABEL_ATTACH_PHOTOS),
            subtitle = AnrealCopy.get(AnrealCopy.ATTACH_PHOTOS_BODY),
            selected = false,
            onClick = {
                onAction(ChatAction.OnPickPhotos)
                onDismiss()
            },
        )
        SheetOption(
            title = AnrealCopy.get(AnrealCopy.LABEL_ATTACH_LOCAL),
            subtitle = AnrealCopy.get(AnrealCopy.ATTACH_LOCAL_BODY),
            selected = false,
            onClick = {
                onAction(ChatAction.OnPickLocalDocument)
                onDismiss()
            },
        )
        SheetOption(
            title = AnrealCopy.get(AnrealCopy.LABEL_ATTACH_LIBRARY),
            subtitle = AnrealCopy.get(AnrealCopy.ATTACH_LIBRARY_BODY),
            selected = false,
            onClick = {
                onAction(ChatAction.OnOpenLibrary)
                onDismiss()
            },
        )
    }
}

@AnrealPreviews
@Composable
private fun ModelAndReasoningSheetPreview() {    AnrealPreview {
        ComposerSheets(
            sheet = ComposerSheet.Model,
            state = chatComposerCatalogPreviewState(),
            onAction = {},
            onDismiss = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun FeaturesSheetPreview() {
    AnrealPreview {
        ComposerSheets(
            sheet = ComposerSheet.Features,
            state = chatComposerCatalogPreviewState(),
            onAction = {},
            onDismiss = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun FeaturesSheetEnhancementsPreview() {
    AnrealPreview {
        ComposerSheets(
            sheet = ComposerSheet.Features,
            state = chatComposerCatalogPreviewState().copy(
                catalogLoading = false,
                capabilities = chatComposerCatalogPreviewState().capabilities.copy(
                    userSkillsCount = 3,
                    userMcpCount = 2,
                ),
                selectedSkillIds = listOf("s1", "s2"),
                selectedMcpServerIds = emptyList(),
            ),
            onAction = {},
            onDismiss = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun AttachSheetPreview() {
    AnrealPreview {
        ComposerSheets(
            sheet = ComposerSheet.Attach,
            state = chatComposerCatalogPreviewState(),
            onAction = {},
            onDismiss = {},
        )
    }
}
