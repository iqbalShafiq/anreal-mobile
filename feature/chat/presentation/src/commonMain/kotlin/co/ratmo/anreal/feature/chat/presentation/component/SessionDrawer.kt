package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.component.AnrealEmpty
import co.ratmo.anreal.core.designsystem.component.AnrealError
import co.ratmo.anreal.core.designsystem.component.AnrealSkeletonList
import co.ratmo.anreal.core.designsystem.component.GlassSurface
import co.ratmo.anreal.core.designsystem.component.GlassTone
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.presentation.AccountUi
import co.ratmo.anreal.feature.chat.presentation.ChatAction
import co.ratmo.anreal.feature.chat.presentation.ChatSessionUi
import co.ratmo.anreal.feature.chat.presentation.ChatState
import co.ratmo.anreal.feature.chat.presentation.groupSessionsByDate
import co.ratmo.anreal.feature.chat.presentation.preview.chatEmptyPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatErrorPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatLoadingPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatWorkspacePreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.previewAccount
import co.ratmo.anreal.feature.chat.presentation.preview.previewReadSession
import co.ratmo.anreal.feature.chat.presentation.preview.previewUnreadSession
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Chat_bubble
import com.composables.icons.materialsymbols.rounded.Description
import com.composables.icons.materialsymbols.rounded.Edit_square
import com.composables.icons.materialsymbols.rounded.Expand_less
import com.composables.icons.materialsymbols.rounded.Folder
import com.composables.icons.materialsymbols.rounded.Image
import com.composables.icons.materialsymbols.rounded.Logout
import com.composables.icons.materialsymbols.rounded.More_vert
import com.composables.icons.materialsymbols.rounded.Settings

@Composable
internal fun SessionDrawer(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    account: AccountUi,
) {
    val newChatDisabled = state.sessions.any {
        it.id == state.selectedSessionId && it.title == AnrealCopy.get(AnrealCopy.ACTION_NEW_CHAT)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        DrawerHeader(
            newChatDisabled = newChatDisabled,
            onNewChat = { onAction(ChatAction.OnNewChat) },
        )
        when {
            state.sessionsLoading && state.sessions.isEmpty() -> {
                Column(modifier = Modifier.weight(1f).padding(horizontal = AnrealSpacing.sm)) {
                    WorkspaceNav(onAction = onAction)
                    AnrealSkeletonList(count = 6, itemHeight = 48.dp)
                }
            }
            state.sessionsError != null && state.sessions.isEmpty() -> {
                Column(modifier = Modifier.weight(1f).padding(horizontal = AnrealSpacing.sm)) {
                    WorkspaceNav(onAction = onAction)
                    AnrealError(
                        message = state.sessionsError.asString(),
                        onRetry = { onAction(ChatAction.OnRefreshSessions) },
                    )
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    item(key = "workspace") {
                        WorkspaceNav(onAction = onAction)
                    }
                    if (state.recentProjects.isNotEmpty()) {
                        item(key = "recent-label") {
                            SectionLabel(AnrealCopy.get(AnrealCopy.LABEL_RECENT_PROJECTS))
                        }
                        items(state.recentProjects, key = { "project-${it.id}" }) { project ->
                            RecentProjectRow(
                                name = project.name,
                                onClick = { onAction(ChatAction.OnOpenRecentProject(project.id)) },
                            )
                        }
                    }
                    if (state.sessions.isEmpty()) {
                        item(key = "empty") {
                            AnrealEmpty(
                                title = AnrealCopy.get(AnrealCopy.CHAT_SESSIONS_EMPTY_TITLE),
                                body = AnrealCopy.get(AnrealCopy.CHAT_SESSIONS_EMPTY_BODY),
                                actionLabel = AnrealCopy.get(AnrealCopy.ACTION_NEW_CHAT),
                                onAction = { onAction(ChatAction.OnNewChat) },
                            )
                        }
                    } else {
                        groupSessionsByDate(state.sessions).forEach { group ->
                            item(key = "label-${group.label}") {
                                SectionLabel(group.label)
                            }
                            items(group.sessions, key = { it.id }) { session ->
                                SessionRow(
                                    session = session,
                                    selected = session.id == state.selectedSessionId,
                                    onClick = { onAction(ChatAction.OnSessionClick(session.id)) },
                                    onRename = { onAction(ChatAction.OnSessionMenuRename(session.id)) },
                                    onDelete = { onAction(ChatAction.OnSessionMenuDelete(session.id)) },
                                )
                            }
                        }
                    }
                }
            }
        }
        AccountFooter(account = account, onAction = onAction)
    }
}

@Composable
private fun DrawerHeader(
    newChatDisabled: Boolean,
    onNewChat: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AnrealSpacing.sm, vertical = AnrealSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "A",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Text(
            text = AnrealCopy.get(AnrealCopy.LABEL_APP_NAME),
            modifier = Modifier.padding(start = AnrealSpacing.sm).weight(1f),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(
            onClick = onNewChat,
            enabled = !newChatDisabled,
        ) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Edit_square,
                contentDescription = AnrealCopy.get(AnrealCopy.ACTION_NEW_CHAT),
            )
        }
    }
}

@Composable
private fun WorkspaceNav(onAction: (ChatAction) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = AnrealSpacing.xs, vertical = AnrealSpacing.xxs)) {
        SectionLabel(AnrealCopy.get(AnrealCopy.LABEL_WORKSPACE))
        WorkspaceItem(
            icon = MaterialSymbols.Rounded.Chat_bubble,
            label = AnrealCopy.get(AnrealCopy.LABEL_ALL_CHATS),
            selected = true,
            onClick = {},
        )
        WorkspaceItem(
            icon = MaterialSymbols.Rounded.Folder,
            label = AnrealCopy.get(AnrealCopy.LABEL_PROJECTS),
            selected = false,
            onClick = { onAction(ChatAction.OnOpenProjects) },
        )
        WorkspaceItem(
            icon = MaterialSymbols.Rounded.Description,
            label = AnrealCopy.get(AnrealCopy.LABEL_DOCUMENTS),
            selected = false,
            onClick = { onAction(ChatAction.OnOpenDocumentsLibrary) },
        )
        WorkspaceItem(
            icon = MaterialSymbols.Rounded.Image,
            label = AnrealCopy.get(AnrealCopy.LABEL_IMAGES),
            selected = false,
            onClick = { onAction(ChatAction.OnOpenImages) },
        )
    }
}

@Composable
private fun WorkspaceItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AnrealSpacing.sm, vertical = AnrealSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(
            start = AnrealSpacing.md,
            end = AnrealSpacing.md,
            top = AnrealSpacing.sm,
            bottom = AnrealSpacing.xxs,
        ),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RecentProjectRow(
    name: String,
    onClick: () -> Unit,
) {
    Text(
        text = name,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AnrealSpacing.md, vertical = AnrealSpacing.sm),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun SessionRow(
    session: ChatSessionUi,
    selected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    menuExpanded: Boolean? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val menuOpen = menuExpanded ?: expanded
    val shape = MaterialTheme.shapes.medium
    val rowModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = AnrealSpacing.xs, vertical = 1.dp)
        .clickable(onClick = onClick)
    Surface(
        modifier = rowModifier,
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        },
    ) {
        SessionRowContent(
            session = session,
            selected = selected,
            menuOpen = menuOpen,
            onToggleMenu = { if (menuExpanded == null) expanded = true },
            onDismissMenu = { if (menuExpanded == null) expanded = false },
            onRename = onRename,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun SessionRowContent(
    session: ChatSessionUi,
    selected: Boolean,
    menuOpen: Boolean,
    onToggleMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(start = AnrealSpacing.sm, end = 2.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = session.title,
            modifier = Modifier.weight(1f),
            style = if (selected) {
                MaterialTheme.typography.labelLarge
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (session.unread && !selected) {
            Surface(
                modifier = Modifier.padding(end = AnrealSpacing.xxs).size(6.dp),
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.primary,
            ) {}
        }
        Box {
            IconButton(onClick = onToggleMenu, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.More_vert,
                    contentDescription = AnrealCopy.get(AnrealCopy.CD_SESSION_MENU),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(
                    text = { Text(AnrealCopy.get(AnrealCopy.ACTION_RENAME)) },
                    onClick = {
                        onDismissMenu()
                        onRename()
                    },
                )
                DropdownMenuItem(
                    text = { Text(AnrealCopy.get(AnrealCopy.ACTION_DELETE)) },
                    onClick = {
                        onDismissMenu()
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun AccountFooter(
    account: AccountUi,
    onAction: (ChatAction) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val initials = accountInitials(account.name, account.email)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AnrealSpacing.md, vertical = AnrealSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
    ) {
        GlassSurface(
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(10.dp),
            tone = GlassTone.Pane,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name.ifBlank { AnrealCopy.get(AnrealCopy.LABEL_APP_NAME) },
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (account.email.isNotBlank()) {
                Text(
                    text = account.email,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Expand_less,
                    contentDescription = AnrealCopy.get(AnrealCopy.CD_ACCOUNT_MENU),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(AnrealCopy.get(AnrealCopy.ACTION_SETTINGS)) },
                    leadingIcon = {
                        Icon(imageVector = MaterialSymbols.Rounded.Settings, contentDescription = null)
                    },
                    onClick = {
                        menuOpen = false
                        onAction(ChatAction.OnOpenSettings)
                    },
                )
                DropdownMenuItem(
                    text = { Text(AnrealCopy.get(AnrealCopy.ACTION_LOG_OUT)) },
                    leadingIcon = {
                        Icon(imageVector = MaterialSymbols.Rounded.Logout, contentDescription = null)
                    },
                    onClick = {
                        menuOpen = false
                        onAction(ChatAction.OnSignOut)
                    },
                )
            }
        }
    }
}

internal fun accountInitials(name: String, email: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val letters = when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}"
        parts.size == 1 && parts.first().length >= 2 -> parts.first().take(2)
        parts.size == 1 -> parts.first().take(1)
        email.isNotBlank() -> email.take(2)
        else -> "A"
    }
    return letters.uppercase()
}

@AnrealPreviews
@Composable
private fun SessionDrawerLoadingPreview() {
    AnrealPreview {
        SessionDrawer(state = chatLoadingPreviewState(), onAction = {}, account = previewAccount)
    }
}

@AnrealPreviews
@Composable
private fun SessionDrawerErrorPreview() {
    AnrealPreview {
        SessionDrawer(state = chatErrorPreviewState(), onAction = {}, account = previewAccount)
    }
}

@AnrealPreviews
@Composable
private fun SessionDrawerEmptyPreview() {
    AnrealPreview {
        SessionDrawer(state = chatEmptyPreviewState(), onAction = {}, account = previewAccount)
    }
}

@AnrealPreviews
@Composable
private fun SessionDrawerPopulatedPreview() {
    AnrealPreview {
        SessionDrawer(state = chatWorkspacePreviewState(), onAction = {}, account = previewAccount)
    }
}

@AnrealPreviews
@Composable
private fun SessionRowUnreadPreview() {
    AnrealPreview {
        SessionRow(
            session = previewUnreadSession,
            selected = false,
            onClick = {},
            onRename = {},
            onDelete = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun SessionRowReadPreview() {
    AnrealPreview {
        SessionRow(
            session = previewReadSession,
            selected = false,
            onClick = {},
            onRename = {},
            onDelete = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun SessionRowSelectedPreview() {
    AnrealPreview {
        SessionRow(
            session = previewUnreadSession,
            selected = true,
            onClick = {},
            onRename = {},
            onDelete = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun SessionRowMenuOpenPreview() {
    AnrealPreview {
        SessionRow(
            session = previewUnreadSession,
            selected = true,
            onClick = {},
            onRename = {},
            onDelete = {},
            menuExpanded = true,
        )
    }
}
