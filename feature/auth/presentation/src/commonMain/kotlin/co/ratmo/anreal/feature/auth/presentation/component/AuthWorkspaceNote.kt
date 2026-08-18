package co.ratmo.anreal.feature.auth.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.component.GlassSurface
import co.ratmo.anreal.core.designsystem.component.GlassTone
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Chat_bubble
import com.composables.icons.materialsymbols.rounded.Folder

enum class AuthWorkspaceNoteKind {
    Returning,
    NewWorkspace,
}

@Composable
fun AuthWorkspaceNote(
    kind: AuthWorkspaceNoteKind,
    modifier: Modifier = Modifier,
) {
    val title = when (kind) {
        AuthWorkspaceNoteKind.Returning -> AnrealCopy.get(AnrealCopy.AUTH_LOGIN_NOTE_TITLE)
        AuthWorkspaceNoteKind.NewWorkspace -> AnrealCopy.get(AnrealCopy.AUTH_REGISTER_NOTE_TITLE)
    }
    val body = when (kind) {
        AuthWorkspaceNoteKind.Returning -> AnrealCopy.get(AnrealCopy.AUTH_LOGIN_NOTE_BODY)
        AuthWorkspaceNoteKind.NewWorkspace -> AnrealCopy.get(AnrealCopy.AUTH_REGISTER_NOTE_BODY)
    }
    val icon = when (kind) {
        AuthWorkspaceNoteKind.Returning -> MaterialSymbols.Rounded.Chat_bubble
        AuthWorkspaceNoteKind.NewWorkspace -> MaterialSymbols.Rounded.Folder
    }

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tone = GlassTone.Pane,
    ) {
        Row(
            modifier = Modifier.padding(AnrealSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@AnrealPreviews
@Composable
private fun ReturningWorkspaceNotePreview() {
    AnrealPreview {
        AuthWorkspaceNote(
            kind = AuthWorkspaceNoteKind.Returning,
            modifier = Modifier.padding(AnrealSpacing.md),
        )
    }
}

@AnrealPreviews
@Composable
private fun NewWorkspaceNotePreview() {
    AnrealPreview {
        AuthWorkspaceNote(
            kind = AuthWorkspaceNoteKind.NewWorkspace,
            modifier = Modifier.padding(AnrealSpacing.md),
        )
    }
}
