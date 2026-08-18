package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.feature.chat.presentation.ChatAction
import co.ratmo.anreal.feature.chat.presentation.ChatState
import coil3.compose.AsyncImage
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Push_pin

@Composable
internal fun SessionImageStrip(state: ChatState, onAction: (ChatAction) -> Unit) {
    if (state.sessionImages.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                AnrealCopy.get(AnrealCopy.LABEL_SESSION_IMAGES),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                AnrealCopy.get(AnrealCopy.IMAGE_CONTEXT_HELP),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm)) {
            items(state.sessionImages, key = { it.id }) { image ->
                Surface(
                    modifier = Modifier
                        .size(width = 112.dp, height = 88.dp)
                        .clickable { onAction(ChatAction.OnToggleImageContext(image.id)) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = if (image.pinned) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
                ) {
                    Box {
                        AsyncImage(
                            model = image.bytes,
                            contentDescription = image.prompt.ifBlank {
                                AnrealCopy.get(AnrealCopy.LABEL_IMAGE)
                            },
                            modifier = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        if (image.pinned) {
                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Icon(
                                    MaterialSymbols.Rounded.Push_pin,
                                    contentDescription = AnrealCopy.get(AnrealCopy.CD_UNPIN_IMAGE),
                                    modifier = Modifier.padding(4.dp).size(16.dp),
                                )
                            }
                        }
                        Text(
                            image.prompt,
                            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}
