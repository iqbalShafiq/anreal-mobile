package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (LocalInspectionMode.current) {
        InspectionBottomSheet(content)
        return
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InspectionBottomSheet(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = BottomSheetDefaults.ExpandedShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = BottomSheetDefaults.Elevation,
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BottomSheetDefaults.DragHandle()
            }
            content()
        }
    }
}

@Composable
internal fun SheetTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AnrealSpacing.md, vertical = AnrealSpacing.sm),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
