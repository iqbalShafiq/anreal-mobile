package co.ratmo.anreal.feature.workspace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.ratmo.anreal.core.designsystem.component.AnrealAtmosphere
import co.ratmo.anreal.core.designsystem.component.AnrealMarkdown
import co.ratmo.anreal.core.designsystem.component.AnrealSkeleton
import co.ratmo.anreal.core.designsystem.component.AnrealSkeletonList
import co.ratmo.anreal.core.designsystem.component.AnrealSegmentedTabs
import co.ratmo.anreal.core.designsystem.component.AnrealTextField
import co.ratmo.anreal.core.designsystem.component.GlassSurface
import co.ratmo.anreal.core.designsystem.component.GlassExtendedFloatingActionButton
import co.ratmo.anreal.core.designsystem.component.GlassTone
import co.ratmo.anreal.core.designsystem.component.GlassTopBar
import co.ratmo.anreal.core.designsystem.component.glassMutedTextColor
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.ObserveAsEvents
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import com.composables.icons.materialsymbols.rounded.Arrow_back
import com.composables.icons.materialsymbols.rounded.Delete
import com.composables.icons.materialsymbols.rounded.Description
import com.composables.icons.materialsymbols.rounded.Folder
import com.composables.icons.materialsymbols.rounded.Image
import com.composables.icons.materialsymbols.rounded.Edit
import com.composables.icons.materialsymbols.rounded.Chevron_left
import com.composables.icons.materialsymbols.rounded.Chevron_right
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun WorkspaceRoot(
    initialSection: WorkspaceSection,
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit = {},
    viewModel: WorkspaceViewModel = koinViewModel { parametersOf(initialSection) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            WorkspaceEvent.NavigateBack -> onBack()
            is WorkspaceEvent.OpenProject -> onOpenProject(event.projectId)
        }
    }
    WorkspaceScreen(state, viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(state: WorkspaceState, onAction: (WorkspaceAction) -> Unit) {
    AnrealAtmosphere {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar(frosted = false) {
                    TopAppBar(
                        title = { Text(AnrealCopy.get(AnrealCopy.LABEL_WORKSPACE)) },
                        navigationIcon = {
                            IconButton(onClick = { onAction(WorkspaceAction.Back) }) {
                                Icon(MaterialSymbols.Rounded.Arrow_back, AnrealCopy.get(AnrealCopy.CD_BACK))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    )
                }
            },
            floatingActionButton = {
                if (state.section == WorkspaceSection.Projects) {
                    GlassExtendedFloatingActionButton(
                        onClick = { onAction(WorkspaceAction.ShowCreateProject) },
                        icon = MaterialSymbols.Rounded.Add,
                        label = AnrealCopy.get(AnrealCopy.ACTION_NEW_PROJECT),
                    )
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.md),
            ) {
                AnrealSegmentedTabs(
                    items = WorkspaceSection.entries,
                    selected = state.section,
                    label = WorkspaceSection::label,
                    onSelect = { onAction(WorkspaceAction.SelectSection(it)) },
                    modifier = Modifier.padding(
                        start = AnrealSpacing.screenCompact,
                        end = AnrealSpacing.screenCompact,
                        top = AnrealSpacing.lg,
                    ),
                )
                if (state.section != WorkspaceSection.Images) {
                    AnrealTextField(
                        value = state.query,
                        onValueChange = { onAction(WorkspaceAction.ChangeQuery(it)) },
                        label = AnrealCopy.get(AnrealCopy.LABEL_SEARCH),
                        placeholder = AnrealCopy.get(AnrealCopy.PLACEHOLDER_SEARCH_WORKSPACE),
                        modifier = Modifier.padding(horizontal = AnrealSpacing.screenCompact),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                }
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    WorkspaceContent(state, onAction)
                }
            }
        }
    }
    if (state.showCreateProject) CreateProjectDialog(state, onAction)
    state.deleteTarget?.let { target -> DeleteWorkspaceDialog(state, target, onAction) }
    if (state.preview != null || state.previewLoading || state.previewError != null) {
        DocumentPreviewDialog(state, onAction)
    }
}

@Composable
private fun WorkspaceContent(state: WorkspaceState, onAction: (WorkspaceAction) -> Unit) {
    when {
        state.isLoading && state.section !in state.loadedSections -> WorkspaceLoading(state.section)
        state.error != null && state.section !in state.loadedSections -> WorkspaceStatus(
            title = state.error.asString(),
            action = AnrealCopy.get(AnrealCopy.ACTION_RETRY),
            onAction = { onAction(WorkspaceAction.Retry) },
        )
        else -> when (state.section) {
            WorkspaceSection.Projects -> WorkspaceList(
                empty = state.projects.isEmpty(),
                emptyText = AnrealCopy.get(AnrealCopy.PROJECTS_EMPTY),
            ) {
                items(state.projects, key = ProjectUi::id) { project ->
                    WorkspaceCard(
                        icon = MaterialSymbols.Rounded.Folder,
                        title = project.name,
                        body = project.description.ifBlank {
                            AnrealCopy.get(AnrealCopy.PROJECT_DESCRIPTION_EMPTY)
                        },
                        detail = "${project.documentCount} documents · ${project.chatCount} chats",
                        onClick = { onAction(WorkspaceAction.OpenProject(project.id)) },
                        onEdit = { onAction(WorkspaceAction.ShowEditProject(project.id)) },
                        onDelete = {
                            onAction(WorkspaceAction.RequestDeleteProject(project.id, project.name))
                        },
                    )
                }
                if (state.nextCursors[WorkspaceSection.Projects] != null) {
                    item(key = "load-more-projects") { LoadMoreRow(state, onAction) }
                }
            }
            WorkspaceSection.Documents -> WorkspaceList(
                empty = state.documents.isEmpty(),
                emptyText = AnrealCopy.get(AnrealCopy.DOCUMENTS_LIBRARY_EMPTY),
            ) {
                items(state.documents, key = DocumentUi::id) { document ->
                    WorkspaceCard(
                        icon = MaterialSymbols.Rounded.Description,
                        title = document.filename,
                        body = document.summary,
                        detail = listOfNotNull(document.projectName, document.detail).joinToString(" · "),
                        onClick = { onAction(WorkspaceAction.OpenDocument(document.id)) },
                        onDelete = {
                            onAction(WorkspaceAction.RequestDeleteDocument(document.id, document.filename))
                        },
                    )
                }
                if (state.nextCursors[WorkspaceSection.Documents] != null) {
                    item(key = "load-more-documents") { LoadMoreRow(state, onAction) }
                }
            }
            WorkspaceSection.Images -> WorkspaceList(
                empty = state.images.isEmpty(),
                emptyText = AnrealCopy.get(AnrealCopy.IMAGES_EMPTY),
            ) {
                items(state.images, key = ImageUi::id) { image ->
                    WorkspaceImageCard(image)
                }
            }
        }
    }
}

@Composable
private fun WorkspaceList(
    empty: Boolean,
    emptyText: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    if (empty) {
        WorkspaceStatus(title = emptyText)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = WorkspaceListPadding,
            verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            content = content,
        )
    }
}

@Composable
private fun WorkspaceCard(
    icon: ImageVector,
    title: String,
    body: String,
    detail: String = "",
    onClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier,
            ),
        tone = GlassTone.Regular,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AnrealSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) { Box(contentAlignment = Alignment.Center) { Icon(icon, null) } }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (body.isNotBlank()) Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = glassMutedTextColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (detail.isNotBlank()) Text(
                    detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = glassMutedTextColor(),
                )
            }
            onEdit?.let { edit ->
                WorkspaceRowAction(
                    icon = MaterialSymbols.Rounded.Edit,
                    contentDescription = AnrealCopy.get(AnrealCopy.ACTION_RENAME),
                    onClick = edit,
                )
            }
            onDelete?.let { delete ->
                WorkspaceRowAction(
                    icon = MaterialSymbols.Rounded.Delete,
                    contentDescription = AnrealCopy.get(AnrealCopy.ACTION_DELETE),
                    onClick = delete,
                )
            }
        }
    }
}

@Composable
private fun WorkspaceRowAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
            Icon(icon, contentDescription)
        }
    }
}

@Composable
private fun LoadMoreRow(state: WorkspaceState, onAction: (WorkspaceAction) -> Unit) {
    TextButton(
        onClick = { onAction(WorkspaceAction.LoadMore) },
        enabled = !state.isLoadingMore,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            AnrealCopy.get(
                if (state.isLoadingMore) AnrealCopy.STATUS_LOADING else AnrealCopy.ACTION_LOAD_MORE,
            ),
        )
    }
}

@Composable
private fun WorkspaceImageCard(image: ImageUi) {
    var loadState by remember(image.id, image.bytes) {
        mutableStateOf(
            when {
                image.loading -> WorkspaceImageLoadState.Loading
                image.bytes == null -> WorkspaceImageLoadState.Error
                else -> WorkspaceImageLoadState.Loading
            },
        )
    }
    val imageRequest = image.bytes?.let { bytes ->
        val context = LocalPlatformContext.current
        remember(image.id, bytes, context) {
            ImageRequest.Builder(context)
                .data(bytes)
                .memoryCacheKey("workspace-image-${image.id}")
                .build()
        }
    }
    val stateDescription = when (loadState) {
        WorkspaceImageLoadState.Loading -> AnrealCopy.get(AnrealCopy.STATUS_LOADING)
        WorkspaceImageLoadState.Loaded -> AnrealCopy.get(AnrealCopy.STATUS_IMAGE_LOADED)
        WorkspaceImageLoadState.Error -> AnrealCopy.get(AnrealCopy.ERROR_IMAGE_LOAD)
    }
    GlassSurface(modifier = Modifier.fillMaxWidth(), tone = GlassTone.Regular) {
        Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm)) {
            if (imageRequest != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 280.dp)
                        .semantics { this.stateDescription = stateDescription },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = image.prompt.ifBlank {
                            AnrealCopy.get(AnrealCopy.LABEL_IMAGE)
                        },
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        onLoading = { loadState = WorkspaceImageLoadState.Loading },
                        onSuccess = { loadState = WorkspaceImageLoadState.Loaded },
                        onError = { loadState = WorkspaceImageLoadState.Error },
                    )
                    when (loadState) {
                        WorkspaceImageLoadState.Loading -> CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                        )
                        WorkspaceImageLoadState.Error -> Text(
                            AnrealCopy.get(AnrealCopy.ERROR_IMAGE_LOAD),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        WorkspaceImageLoadState.Loaded -> Unit
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp)
                        .semantics { this.stateDescription = stateDescription },
                    contentAlignment = Alignment.Center,
                ) {
                    if (image.loading) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                    } else {
                        Text(
                            AnrealCopy.get(AnrealCopy.ERROR_IMAGE_LOAD),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(AnrealSpacing.md)) {
                Text(
                    image.prompt.ifBlank { AnrealCopy.get(AnrealCopy.LABEL_IMAGE) },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    image.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = glassMutedTextColor(),
                )
            }
        }
    }
}

private enum class WorkspaceImageLoadState {
    Loading,
    Loaded,
    Error,
}

@Composable
private fun WorkspaceStatus(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().padding(AnrealSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = glassMutedTextColor())
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun CreateProjectDialog(state: WorkspaceState, onAction: (WorkspaceAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(WorkspaceAction.DismissCreateProject) },
        title = {
            Text(
                AnrealCopy.get(
                    if (state.editProjectId == null) AnrealCopy.ACTION_NEW_PROJECT else AnrealCopy.ACTION_RENAME,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm)) {
                AnrealTextField(
                    value = state.projectName,
                    onValueChange = { onAction(WorkspaceAction.ChangeProjectName(it)) },
                    label = AnrealCopy.get(AnrealCopy.LABEL_NAME),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                AnrealTextField(
                    value = state.projectDescription,
                    onValueChange = { onAction(WorkspaceAction.ChangeProjectDescription(it)) },
                    label = AnrealCopy.get(AnrealCopy.LABEL_DESCRIPTION),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                state.mutationError?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(WorkspaceAction.CreateProject) },
                enabled = !state.isMutating,
            ) {
                Text(
                    AnrealCopy.get(
                        if (state.editProjectId == null) AnrealCopy.ACTION_CREATE else AnrealCopy.ACTION_RENAME,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(WorkspaceAction.DismissCreateProject) },
                enabled = !state.isMutating,
            ) { Text(AnrealCopy.get(AnrealCopy.ACTION_CANCEL)) }
        },
    )
}

@Composable
private fun DocumentPreviewDialog(state: WorkspaceState, onAction: (WorkspaceAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(WorkspaceAction.CloseDocument) },
        title = {
            Text(state.preview?.filename ?: AnrealCopy.get(AnrealCopy.LABEL_DOCUMENT_PREVIEW))
        },
        text = {
            when {
                state.previewLoading -> AnrealSkeletonList(count = 4, itemHeight = 64.dp)
                state.previewError != null -> WorkspaceStatus(
                    title = state.previewError.asString(),
                    action = AnrealCopy.get(AnrealCopy.ACTION_RETRY),
                    onAction = { onAction(WorkspaceAction.RetryDocumentPreview) },
                )
                state.preview != null -> LazyColumn(
                    modifier = Modifier.heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
                ) {
                    item { Text(state.preview.summary, style = MaterialTheme.typography.bodyMedium) }
                    items(state.preview.images) { bytes ->
                        AsyncImage(
                            model = bytes,
                            contentDescription = AnrealCopy.get(AnrealCopy.LABEL_DOCUMENT_IMAGE),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    item { AnrealMarkdown(state.preview.markdown) }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = {
                                    onAction(WorkspaceAction.OpenDocumentPage(state.preview.pageIndex - 1))
                                },
                                enabled = state.preview.pageIndex > 0,
                            ) {
                                Icon(MaterialSymbols.Rounded.Chevron_left, AnrealCopy.get(AnrealCopy.ACTION_PREVIOUS))
                            }
                            Text("${state.preview.pageIndex + 1} / ${state.preview.pageCount}")
                            IconButton(
                                onClick = {
                                    onAction(WorkspaceAction.OpenDocumentPage(state.preview.pageIndex + 1))
                                },
                                enabled = state.preview.pageIndex + 1 < state.preview.pageCount,
                            ) {
                                Icon(MaterialSymbols.Rounded.Chevron_right, AnrealCopy.get(AnrealCopy.ACTION_NEXT))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAction(WorkspaceAction.CloseDocument) }) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_CLOSE))
            }
        },
    )
}

@Composable
private fun DeleteWorkspaceDialog(
    state: WorkspaceState,
    target: WorkspaceDeleteTarget,
    onAction: (WorkspaceAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(WorkspaceAction.DismissDelete) },
        title = { Text(AnrealCopy.get(AnrealCopy.DIALOG_DELETE_WORKSPACE_TITLE)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
                Text(AnrealCopy.get(AnrealCopy.DIALOG_DELETE_WORKSPACE_BODY).replace("{0}", target.label))
                state.mutationError?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAction(WorkspaceAction.ConfirmDelete) }, enabled = !state.isMutating) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_DELETE))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(WorkspaceAction.DismissDelete) }, enabled = !state.isMutating) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_CANCEL))
            }
        },
    )
}

@Composable
private fun WorkspaceLoading(section: WorkspaceSection) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = WorkspaceListPadding,
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
    ) {
        if (section == WorkspaceSection.Images) {
            items(3) { WorkspaceImageSkeleton() }
        } else {
            items(6) { AnrealSkeleton(height = 72.dp) }
        }
    }
}

@Composable
private fun WorkspaceImageSkeleton() {
    GlassSurface(modifier = Modifier.fillMaxWidth(), tone = GlassTone.Regular) {
        Column {
            AnrealSkeleton(height = 160.dp)
            Column(
                modifier = Modifier.padding(AnrealSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
            ) {
                AnrealSkeleton(height = 16.dp)
                AnrealSkeleton(height = 12.dp)
            }
        }
    }
}

private val WorkspaceListPadding = PaddingValues(
    start = AnrealSpacing.screenCompact,
    end = AnrealSpacing.screenCompact,
    bottom = 120.dp,
)

private fun WorkspaceSection.label(): String = when (this) {
    WorkspaceSection.Projects -> AnrealCopy.get(AnrealCopy.LABEL_PROJECTS)
    WorkspaceSection.Documents -> AnrealCopy.get(AnrealCopy.LABEL_DOCUMENTS)
    WorkspaceSection.Images -> AnrealCopy.get(AnrealCopy.LABEL_IMAGES)
}

@AnrealPreviews
@Composable
private fun WorkspaceProjectsPreview() {
    AnrealPreview {
        WorkspaceScreen(
            state = WorkspaceState(
                projects = listOf(ProjectUi("p1", "Quarterly review", "Board sources", 2, 4)),
                loadedSections = setOf(WorkspaceSection.Projects),
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun WorkspaceDocumentsEmptyPreview() {
    AnrealPreview {
        WorkspaceScreen(
            state = WorkspaceState(
                section = WorkspaceSection.Documents,
                loadedSections = setOf(WorkspaceSection.Documents),
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun WorkspaceImagesLoadingPreview() {
    AnrealPreview {
        WorkspaceScreen(
            state = WorkspaceState(
                section = WorkspaceSection.Images,
                isLoading = true,
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun WorkspaceImagesErrorPreview() {
    AnrealPreview {
        WorkspaceScreen(
            state = WorkspaceState(
                section = WorkspaceSection.Images,
                error = UiText.DynamicString("Could not load images"),
            ),
            onAction = {},
        )
    }
}
