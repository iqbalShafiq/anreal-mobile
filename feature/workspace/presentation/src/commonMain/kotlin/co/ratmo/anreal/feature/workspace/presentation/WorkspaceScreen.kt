package co.ratmo.anreal.feature.workspace.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import co.ratmo.anreal.core.designsystem.component.AnrealAtmosphereBackground
import co.ratmo.anreal.core.designsystem.component.AnrealLoadingIndicator
import co.ratmo.anreal.core.designsystem.component.AnrealMarkdown
import co.ratmo.anreal.core.designsystem.component.AnrealBottomSheet
import co.ratmo.anreal.core.designsystem.component.AnrealSearchField
import co.ratmo.anreal.core.designsystem.component.AnrealSheetTitle
import co.ratmo.anreal.core.designsystem.component.AnrealSkeletonCard
import co.ratmo.anreal.core.designsystem.component.AnrealSkeletonImageCard
import co.ratmo.anreal.core.designsystem.component.AnrealSkeletonList
import co.ratmo.anreal.core.designsystem.component.AnrealSegmentedTabs
import co.ratmo.anreal.core.designsystem.component.AnrealTextField
import co.ratmo.anreal.core.designsystem.component.glassFaintTextColor
import co.ratmo.anreal.core.designsystem.component.glassMutedTextColor
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.ObserveAsEvents
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.workspace.domain.WorkspaceProjectSort
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import com.composables.icons.materialsymbols.rounded.Arrow_back
import com.composables.icons.materialsymbols.rounded.Delete
import com.composables.icons.materialsymbols.rounded.More_horiz
import com.composables.icons.materialsymbols.rounded.Description
import com.composables.icons.materialsymbols.rounded.Folder
import com.composables.icons.materialsymbols.rounded.Grid_view
import com.composables.icons.materialsymbols.rounded.Image
import com.composables.icons.materialsymbols.rounded.Edit
import com.composables.icons.materialsymbols.rounded.Chevron_left
import com.composables.icons.materialsymbols.rounded.Chevron_right
import com.composables.icons.materialsymbols.rounded.View_list
import com.composables.icons.materialsymbols.rounded.Visibility
import com.composables.icons.materialsymbols.rounded.Share
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun WorkspaceRoot(
    initialSection: WorkspaceSection,
    onBack: () -> Unit,
    onOpenProject: (projectId: String, name: String) -> Unit = { _, _ -> },
    scopeSessionId: String? = null,
    onOpenSiteOrigin: (siteId: String, sessionId: String) -> Unit = { _, _ -> },
    onContinueSite: (siteId: String, sessionId: String) -> Unit = { _, _ -> },
    viewModel: WorkspaceViewModel = koinViewModel { parametersOf(initialSection, scopeSessionId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            WorkspaceEvent.NavigateBack -> onBack()
            is WorkspaceEvent.OpenProject -> onOpenProject(event.projectId, event.name)
            is WorkspaceEvent.OpenSiteOrigin -> onOpenSiteOrigin(event.siteId, event.sessionId)
            is WorkspaceEvent.ContinueSite -> onContinueSite(event.siteId, event.sessionId)
        }
    }
    WorkspaceScreen(state, viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(state: WorkspaceState, onAction: (WorkspaceAction) -> Unit) {
    AnrealAtmosphere(
        background = AnrealAtmosphereBackground.Surface,
        animateBackground = false,
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(AnrealCopy.get(AnrealCopy.LABEL_WORKSPACE)) },
                    navigationIcon = {
                        IconButton(onClick = { onAction(WorkspaceAction.Back) }) {
                            Icon(MaterialSymbols.Rounded.Arrow_back, AnrealCopy.get(AnrealCopy.CD_BACK))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
            floatingActionButton = {
                if (state.section == WorkspaceSection.Projects) {
                    ExtendedFloatingActionButton(
                        text = { Text(AnrealCopy.get(AnrealCopy.ACTION_NEW_PROJECT)) },
                        icon = { Icon(MaterialSymbols.Rounded.Add, contentDescription = null) },
                        onClick = { onAction(WorkspaceAction.ShowCreateProject) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 3.dp,
                            pressedElevation = 6.dp,
                        ),
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
                    glass = false,
                    containerColor = Color.Transparent,
                    modifier = Modifier.padding(
                        start = AnrealSpacing.screenCompact,
                        end = AnrealSpacing.screenCompact,
                        top = AnrealSpacing.md,
                    ),
                )
                if (state.section != WorkspaceSection.Images && state.section != WorkspaceSection.Sites &&
                    state.section != WorkspaceSection.Tasks && state.section != WorkspaceSection.Schedules
                ) {
                    AnrealSearchField(
                        value = state.query,
                        onValueChange = { onAction(WorkspaceAction.ChangeQuery(it)) },
                        placeholder = AnrealCopy.get(AnrealCopy.PLACEHOLDER_SEARCH_WORKSPACE),
                        contentDescription = AnrealCopy.get(AnrealCopy.CD_WORKSPACE_SEARCH),
                        modifier = Modifier.padding(horizontal = AnrealSpacing.screenCompact),
                        glass = false,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AnrealSpacing.screenCompact),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.section == WorkspaceSection.Projects) {
                        ProjectSortChips(
                            selected = state.projectSort,
                            onSelect = { onAction(WorkspaceAction.SetProjectSort(it)) },
                        )
                    } else if (state.section == WorkspaceSection.Images) {
                        ViewModeToggle(
                            selected = state.viewMode,
                            onSelect = { onAction(WorkspaceAction.SetViewMode(it)) },
                        )
                        val count = when (state.section) {
                            WorkspaceSection.Documents -> state.documents.size
                            WorkspaceSection.Images -> state.images.size
                            WorkspaceSection.Sites -> state.sites.size
                            WorkspaceSection.Tasks -> state.tasks.size
                            WorkspaceSection.Schedules -> state.schedules.size
                            WorkspaceSection.Artifacts -> state.artifacts.size
                            WorkspaceSection.Projects -> 0
                        }
                        WorkspaceLoadedLabel(
                            count = count,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = AnrealSpacing.md),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        )
                    } else {
                        WorkspaceLoadedLabel(
                            count = state.documents.size,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    WorkspaceContent(state, onAction)
                }
            }
        }
    }
    if (state.showCreateProject) CreateProjectDialog(state, onAction)
    state.deleteTarget?.let { target -> DeleteWorkspaceDialog(state, target, onAction) }
    if (state.cardSheetTarget != null) {
        WorkspaceCardOptionsSheet(
            target = state.cardSheetTarget,
            onDismiss = { onAction(WorkspaceAction.DismissCardSheet) },
        )
    }
}
@Composable
private fun WorkspaceContent(state: WorkspaceState, onAction: (WorkspaceAction) -> Unit) {
    when {
        state.isLoading && state.section !in state.loadedSections -> WorkspaceLoading(section = state.section, viewMode = state.viewMode)
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
                        detail = "${project.documentCount} documents · ${project.chatCount} chats",
                        onClick = { onAction(WorkspaceAction.OpenProject(project.id)) },
                        onMore = {
                            onAction(WorkspaceAction.ShowCardSheet(
                                WorkspaceCardSheetTarget.Project(
                                    id = project.id,
                                    label = project.name,
                                    description = project.description,
                                    documentCount = project.documentCount,
                                ),
                            ))
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
                        detail = listOfNotNull(document.projectName, document.detail).joinToString(" · "),
                        onClick = { onAction(WorkspaceAction.OpenDocument(document.id)) },
                        onMore = {
                            onAction(WorkspaceAction.ShowCardSheet(
                                WorkspaceCardSheetTarget.Document(document.id, document.filename),
                            ))
                        },
                    )
                }
                if (state.nextCursors[WorkspaceSection.Documents] != null) {
                    item(key = "load-more-documents") { LoadMoreRow(state, onAction) }
                }
            }
            WorkspaceSection.Images -> if (state.viewMode == WorkspaceViewMode.Grid) {
                if (state.images.isEmpty()) {
                    WorkspaceStatus(title = AnrealCopy.get(AnrealCopy.IMAGES_EMPTY))
                } else {
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = WorkspaceListPadding,
                        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
                    ) {
                        items(state.images.size, key = { state.images[it].id }) { index ->
                            WorkspaceImageCard(state.images[index], compact = true)
                        }
                    }
                }
            } else {
                WorkspaceList(
                    empty = state.images.isEmpty(),
                    emptyText = AnrealCopy.get(AnrealCopy.IMAGES_EMPTY),
                ) {
                    items(state.images, key = ImageUi::id) { image ->
                        WorkspaceImageCard(image)
                    }
                }
            }
            WorkspaceSection.Sites -> ScopeSitesPanel(
                sites = state.sites,
                isLoading = state.isLoading,
                loaded = WorkspaceSection.Sites in state.loadedSections,
                hasScope = state.scopeSessionId != null,
                error = state.error,
                baseUrl = state.siteBaseUrl,
                previewSiteId = state.previewSiteId,
                onAction = onAction,
            )
            WorkspaceSection.Tasks -> TasksPanel(
                tasks = state.tasks,
                isLoading = state.isLoading,
                loaded = WorkspaceSection.Tasks in state.loadedSections,
                hasScope = state.scopeSessionId != null,
                error = state.error,
                editor = state.taskEditor,
                isMutating = state.isMutating,
                mutationError = state.mutationError,
                onAction = onAction,
            )
            WorkspaceSection.Schedules -> SchedulesPanel(
                schedules = state.schedules,
                isLoading = state.isLoading,
                loaded = WorkspaceSection.Schedules in state.loadedSections,
                hasScope = state.scopeSessionId != null,
                error = state.error,
                editor = state.scheduleEditor,
                isMutating = state.isMutating,
                mutationError = state.mutationError,
                onAction = onAction,
            )
            WorkspaceSection.Artifacts -> ArtifactsBrowser(
                artifacts = state.artifacts,
                isLoading = state.isLoading,
                loaded = WorkspaceSection.Artifacts in state.loadedSections,
                hasScope = state.scopeSessionId != null,
                error = state.error,
                typeFilter = state.artifactTypeFilter,
                detail = state.artifactDetail,
                captionDraft = state.captionDraft,
                captionSaving = state.captionSaving,
                captionError = state.captionError,
                onAction = onAction,
            )
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
    detail: String = "",
    onClick: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier,
            ),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
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
            if (detail.isNotBlank()) Text(
                    detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = glassMutedTextColor(),
                )
            }
            onMore?.let { more ->
                WorkspaceRowAction(
                    icon = MaterialSymbols.Rounded.More_horiz,
                    contentDescription = AnrealCopy.get(AnrealCopy.CD_CARD_ACTIONS),
                    onClick = more,
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
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
    ) {
        Icon(icon, contentDescription, modifier = Modifier.size(20.dp))
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
private fun WorkspaceImageCard(
    image: ImageUi,
    compact: Boolean = false,
) {
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) AnrealSpacing.xs else AnrealSpacing.sm)) {
            if (imageRequest != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (compact) {
                                Modifier.aspectRatio(1.62f)
                            } else {
                                Modifier.heightIn(min = 160.dp, max = 280.dp)
                            },
                        )
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
                        WorkspaceImageLoadState.Loading -> AnrealLoadingIndicator(
                            modifier = Modifier.size(if (compact) 24.dp else 32.dp),
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
                        .then(
                            if (compact) {
                                Modifier.aspectRatio(1.62f)
                            } else {
                                Modifier.heightIn(min = 160.dp)
                            },
                        )
                        .semantics { this.stateDescription = stateDescription },
                    contentAlignment = Alignment.Center,
                ) {
                    if (image.loading) {
                        AnrealLoadingIndicator(modifier = Modifier.size(if (compact) 24.dp else 32.dp))
                    } else {
                        Text(
                            AnrealCopy.get(AnrealCopy.ERROR_IMAGE_LOAD),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(
                    horizontal = if (compact) AnrealSpacing.xs else AnrealSpacing.md,
                    vertical = if (compact) AnrealSpacing.xs else AnrealSpacing.md,
                ),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs),
            ) {
                Text(
                    image.prompt.ifBlank { AnrealCopy.get(AnrealCopy.LABEL_IMAGE) },
                    style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    image.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = glassMutedTextColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                    placeholder = AnrealCopy.get(AnrealCopy.PLACEHOLDER_PROJECT_NAME),
                    glass = false,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                AnrealTextField(
                    value = state.projectDescription,
                    onValueChange = { onAction(WorkspaceAction.ChangeProjectDescription(it)) },
                    label = AnrealCopy.get(AnrealCopy.LABEL_DESCRIPTION),
                    placeholder = AnrealCopy.get(AnrealCopy.PLACEHOLDER_PROJECT_DESCRIPTION),
                    glass = false,
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
private fun WorkspaceCardOptionsSheet(
    target: WorkspaceCardSheetTarget,
    onDismiss: () -> Unit,
) {
    AnrealBottomSheet(onDismiss = onDismiss) {
        if (target is WorkspaceCardSheetTarget.Project) {
            DropdownMenuSheetContent(
                title = AnrealCopy.get(AnrealCopy.LABEL_PROJECT_ACTIONS),
                subtitle = listOfNotNull(
                    target.label,
                    target.description.takeIf(String::isNotBlank),
                    "${target.documentCount} documents",
                ).joinToString(" · "),
                items = listOf(
                    SheetItem(AnrealCopy.get(AnrealCopy.ACTION_VIEW_PROJECT), AnrealCopy.get(AnrealCopy.ACTION_VIEW_PROJECT_DESCRIPTION), MaterialSymbols.Rounded.Visibility, false, {}),
                    SheetItem(AnrealCopy.get(AnrealCopy.ACTION_RENAME), AnrealCopy.get(AnrealCopy.ACTION_RENAME_DESCRIPTION), MaterialSymbols.Rounded.Edit, false, {}),
                    SheetItem(AnrealCopy.get(AnrealCopy.ACTION_SHARE), AnrealCopy.get(AnrealCopy.ACTION_SHARE_DESCRIPTION), MaterialSymbols.Rounded.Share, false, {}),
                    SheetItem(AnrealCopy.get(AnrealCopy.ACTION_DELETE), AnrealCopy.get(AnrealCopy.ACTION_DELETE_DESCRIPTION), MaterialSymbols.Rounded.Delete, true, {}),
                ),
            )
        } else {
            DropdownMenuSheetContent(
                title = AnrealCopy.get(AnrealCopy.LABEL_DOCUMENT_ACTIONS),
                subtitle = target.label,
                items = listOf(
                    SheetItem(AnrealCopy.get(AnrealCopy.ACTION_VIEW_DOCUMENT), AnrealCopy.get(AnrealCopy.ACTION_VIEW_DOCUMENT_DESCRIPTION), MaterialSymbols.Rounded.Visibility, false, {}),
                    SheetItem(AnrealCopy.get(AnrealCopy.ACTION_SHARE), AnrealCopy.get(AnrealCopy.ACTION_SHARE_DESCRIPTION), MaterialSymbols.Rounded.Share, false, {}),
                    SheetItem(AnrealCopy.get(AnrealCopy.ACTION_DELETE), AnrealCopy.get(AnrealCopy.ACTION_DELETE_DESCRIPTION), MaterialSymbols.Rounded.Delete, true, {}),
                ),
            )
        }
    }
}

@Composable
private fun DropdownMenuSheetContent(
    title: String,
    subtitle: String = "",
    items: List<SheetItem>,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = AnrealSpacing.lg),
    ) {
        AnrealSheetTitle(text = title)
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = glassMutedTextColor(),
                modifier = Modifier.padding(horizontal = AnrealSpacing.md),
            )
        }
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = item.onClick)
                    .padding(horizontal = AnrealSpacing.md, vertical = AnrealSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.large,
                    color = if (item.destructive) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.24f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (item.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs)) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = MaterialTheme.typography.bodyMedium.fontWeight ?: androidx.compose.ui.text.font.FontWeight.Medium,
                        color = if (item.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = glassMutedTextColor(),
                    )
                }
                Icon(
                    imageVector = MaterialSymbols.Rounded.Chevron_right,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = glassFaintTextColor(),
                )
            }
        }
    }
}

private data class SheetItem(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val destructive: Boolean,
    val onClick: () -> Unit,
)

@Composable
private fun WorkspaceLoading(
    section: WorkspaceSection,
    viewMode: WorkspaceViewMode = WorkspaceViewMode.List,
) {
    if (section == WorkspaceSection.Images && viewMode == WorkspaceViewMode.Grid) {
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = WorkspaceListPadding,
            verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
        ) {
            items(4) { AnrealSkeletonImageCard() }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = WorkspaceListPadding,
            verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
        ) {
            if (section == WorkspaceSection.Images) {
                items(3) { AnrealSkeletonImageCard() }
            } else {
                items(6) { AnrealSkeletonCard() }
            }
        }
    }
}

@Composable
private fun WorkspaceLoadedLabel(
    count: Int,
    modifier: Modifier = Modifier,
    textAlign: androidx.compose.ui.text.style.TextAlign,
) {
    Text(
        text = "$count ${AnrealCopy.get(AnrealCopy.LABEL_LOADED)} · ${AnrealCopy.get(AnrealCopy.ACTION_PULL_TO_REFRESH)}",
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = glassMutedTextColor(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
    )
}

@Composable
private fun ProjectSortChips(
    selected: WorkspaceProjectSort,
    onSelect: (WorkspaceProjectSort) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
        val chips = listOf(
            AnrealCopy.get(AnrealCopy.LABEL_SORT_UPDATED) to WorkspaceProjectSort.UpdatedAt,
            AnrealCopy.get(AnrealCopy.LABEL_SORT_OPENED) to WorkspaceProjectSort.LastOpenedAt,
            AnrealCopy.get(AnrealCopy.LABEL_SORT_NAME) to WorkspaceProjectSort.Name,
        )
        chips.forEach { (label, sort) ->
            val isSelected = sort == selected
            Surface(
                onClick = { onSelect(sort) },
                modifier = Modifier
                    .widthIn(min = AnrealSpacing.chipMinWidth)
                    .heightIn(min = AnrealSpacing.touch),
                shape = MaterialTheme.shapes.extraLarge,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.82f)
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.56f)
                    },
                ),
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = AnrealSpacing.touch)
                        .padding(horizontal = AnrealSpacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewModeToggle(
    selected: WorkspaceViewMode,
    onSelect: (WorkspaceViewMode) -> Unit,
) {
    Surface(
        modifier = Modifier.padding(vertical = AnrealSpacing.xs),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = AnrealSpacing.xxs,
                vertical = AnrealSpacing.xxs,
            ),
        ) {
            ViewModeButton(
                selected = selected == WorkspaceViewMode.Grid,
                icon = MaterialSymbols.Rounded.Grid_view,
                contentDescription = AnrealCopy.get(AnrealCopy.CD_GRID_VIEW),
                onClick = { onSelect(WorkspaceViewMode.Grid) },
            )
            ViewModeButton(
                selected = selected == WorkspaceViewMode.List,
                icon = MaterialSymbols.Rounded.View_list,
                contentDescription = AnrealCopy.get(AnrealCopy.CD_LIST_VIEW),
                onClick = { onSelect(WorkspaceViewMode.List) },
            )
        }
    }
}

@Composable
private fun ViewModeButton(
    selected: Boolean,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(AnrealSpacing.touch),
        shape = CircleShape,
        color = Color.Transparent,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            glassMutedTextColor()
                        },
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

internal val WorkspaceListPadding = PaddingValues(
    start = AnrealSpacing.screenCompact,
    // The parent column already supplies the control-to-content gap. Keeping
    // this edge flush makes that gap match the search-to-control spacing.
    top = 0.dp,
    end = AnrealSpacing.screenCompact,
    bottom = AnrealSpacing.xxxl,
)

private fun WorkspaceSection.label(): String = when (this) {
    WorkspaceSection.Projects -> AnrealCopy.get(AnrealCopy.LABEL_PROJECTS)
    WorkspaceSection.Documents -> AnrealCopy.get(AnrealCopy.LABEL_DOCUMENTS)
    WorkspaceSection.Images -> AnrealCopy.get(AnrealCopy.LABEL_IMAGES)
    WorkspaceSection.Sites -> AnrealCopy.get(AnrealCopy.LABEL_SITES)
    WorkspaceSection.Tasks -> AnrealCopy.get(AnrealCopy.LABEL_TASKS)
    WorkspaceSection.Schedules -> AnrealCopy.get(AnrealCopy.LABEL_SCHEDULES)
    WorkspaceSection.Artifacts -> AnrealCopy.get(AnrealCopy.LABEL_ARTIFACTS)
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
