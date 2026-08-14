package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing

@Composable
fun AnrealSkeleton(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    contentDescription: String = "Loading",
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .semantics { this.contentDescription = contentDescription },
    )
}

@Composable
fun AnrealSkeletonList(
    modifier: Modifier = Modifier,
    count: Int = 4,
    itemHeight: Dp = 16.dp,
    contentDescription: String = "Loading",
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
    ) {
        repeat(count) { index ->
            AnrealSkeleton(
                height = itemHeight,
                contentDescription = if (index == 0) contentDescription else "",
            )
        }
    }
}

@AnrealPreviews
@Composable
private fun AnrealSkeletonPreview() {
    AnrealPreview {
        AnrealSkeleton(modifier = Modifier.padding(AnrealSpacing.md))
    }
}

@AnrealPreviews
@Composable
private fun AnrealSkeletonListPreview() {
    AnrealPreview {
        AnrealSkeletonList(
            modifier = Modifier.padding(AnrealSpacing.md),
            count = 4,
            itemHeight = 48.dp,
        )
    }
}
